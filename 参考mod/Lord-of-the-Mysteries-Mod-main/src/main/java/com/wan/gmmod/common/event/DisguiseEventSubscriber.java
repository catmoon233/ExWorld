package com.wan.gmmod.common.event;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.common.capability.data.DisguiseData;
import com.wan.gmmod.content.disguise.DisguiseManager;
import com.wan.gmmod.content.disguise.HumanoidDisguises;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 变形系统（无面人 · 序列 6）相关事件监听：
 * <ul>
 *   <li>击败解锁：玩家亲手击杀人形怪物后，自动加入其变形外观；</li>
 *   <li>观察解锁：潜行 + 右键人形怪物即可记录外观（无需击杀）；</li>
 *   <li>初始赠送：拥有变形能力却无任何外观时，赠送僵尸 + 骷髅；</li>
 *   <li>家族中立：变形为僵尸 / 猪灵家族时，附近同族怪物放弃将其作为目标。</li>
 * </ul>
 */
@EventBusSubscriber(modid = GuimiMod.MODID)
public class DisguiseEventSubscriber {

    /** 击败解锁：玩家亲手击杀人形怪物 → 解锁该外观。 */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        EntityType<?> type = event.getEntity().getType();
        if (!HumanoidDisguises.isHumanoid(type) || !DisguiseManager.hasDisguiseAbility(player)) {
            return;
        }
        DisguiseManager.unlock(player, EntityType.getKey(type), false);
    }

    /** 观察解锁：潜行 + 右键人形怪物即记录其外观（复用现有交互机制）。 */
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || !player.isShiftKeyDown()) {
            return;
        }
        EntityType<?> type = event.getTarget().getType();
        if (!HumanoidDisguises.isHumanoid(type) || !DisguiseManager.hasDisguiseAbility(player)) {
            return;
        }
        DisguiseManager.unlock(player, EntityType.getKey(type), false);
    }

    /** 拥有变形能力却无任何外观时，赠送基础外观（仅触发一次）。 */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || player.tickCount % 100 != 0) {
            return;
        }
        if (DisguiseManager.hasDisguiseAbility(player)
                && player.getData(ModAttachments.UNLOCKED_MOB_DISGUISES).ids().isEmpty()) {
            DisguiseManager.grantInitialGifts(player);
        }
    }

    /**
     * 家族中立：变形为僵尸家族时僵尸不敌视，变形为猪灵家族时猪灵不敌视。
     * 类似原版佩戴僵尸 / 猪灵头颅的效果。
     */
    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getNewAboutToBeSetTarget() instanceof Player player)) {
            return;
        }
        DisguiseData data = player.getData(ModAttachments.DISGUISE_STATE);
        if (!data.isMob()) {
            return;
        }
        ResourceLocation mobId = data.mobId();
        HumanoidDisguises.Entry entry = HumanoidDisguises.get(mobId);
        if (entry == null) {
            return;
        }
        LivingEntity attacker = event.getEntity();
        if ((entry.neutralToZombies() && attacker instanceof Zombie)
                || (entry.neutralToPiglins() && attacker instanceof AbstractPiglin)) {
            event.setCanceled(true);
        }
    }
}
