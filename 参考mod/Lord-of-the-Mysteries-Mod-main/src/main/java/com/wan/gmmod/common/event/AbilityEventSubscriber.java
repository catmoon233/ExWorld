package com.wan.gmmod.common.event;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.common.registry.ModItems;
import com.wan.gmmod.content.abilities.Ability;
import com.wan.gmmod.content.abilities.AbilityRegistry;
import com.wan.gmmod.content.abilities.SkillManager;
import com.wan.gmmod.content.marionette.MarionetteManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * 序列能力相关的游戏事件监听：
 * <ul>
 *   <li>伤害转移（魔术师被动）：虚假血量优先吸收伤害，致命伤转为 30 点虚假血量；</li>
 *   <li>格斗精通（小丑被动）：大幅减少摔落伤害；</li>
 *   <li>飞牌：手持纸右键直接触发（走技能栏同一套校验）；</li>
 *   <li>秘偶命令：空手右键目标 / 秘偶本身。</li>
 * </ul>
 */
@EventBusSubscriber(modid = GuimiMod.MODID)
public class AbilityEventSubscriber {
    /** 致命伤转移后的虚假血量总量 */
    private static final int FAKE_HEALTH_ON_FATAL = 30;

    /** 伤害转移：虚假血量优先吸收；虚假血量为 0 时的致命伤转写为虚假血量。 */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) {
            return;
        }
        if (!SkillManager.isUnlocked(player, GuimiMod.id("damage_transfer"))) {
            return;
        }
        int fake = player.getData(ModAttachments.FAKE_HEALTH);
        float amount = event.getAmount();
        if (fake > 0) {
            // 虚假血量存在期间：伤害优先扣除虚假血量
            int absorbed = (int) Math.min(fake, Math.ceil(amount));
            player.setData(ModAttachments.FAKE_HEALTH, fake - absorbed);
            float remaining = Math.max(0.0F, amount - absorbed);
            event.setAmount(remaining);
            if (remaining <= 0.0F) {
                event.setCanceled(true);
            }
        } else if (amount >= player.getHealth()) {
            // 致命伤：不会立即死亡，转为 30 点虚假血量持续扣除
            event.setCanceled(true);
            player.setHealth(1.0F);
            player.setData(ModAttachments.FAKE_HEALTH, FAKE_HEALTH_ON_FATAL);
            player.displayClientMessage(
                    Component.translatable("ability.guimi_mod.damage_transfer.trigger"), true);
        }
    }

    /** 格斗精通：摔落伤害大幅减少（-80%）。 */
    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player
                && SkillManager.isUnlocked(player, GuimiMod.id("fighting_mastery"))) {
            event.setDamageMultiplier(event.getDamageMultiplier() * 0.2F);
        }
    }

    /** 手持纸类物品右键 → 直接触发「飞牌」（消耗 / 冷却与技能栏一致）。 */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        // 骨骼软化钻洞期间无法使用物品
        if (player.getData(ModAttachments.BONE_CRAWLING)) {
            event.setCanceled(true);
            return;
        }
        if (!event.getItemStack().is(Items.PAPER)
                && !event.getItemStack().is(ModItems.PAPER_CARD.get())) {
            return;
        }
        if (!SkillManager.isUnlocked(player, GuimiMod.id("flying_card"))) {
            return;
        }
        if (!player.level().isClientSide) {
            Ability ability = AbilityRegistry.getById(GuimiMod.id("flying_card"));
            if (ability != null) {
                SkillManager.triggerAbility(player, ability);
            }
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(player.level().isClientSide));
    }

    /** 空手右键实体：秘偶命令（攻击目标 / 释放秘偶）。 */
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (!player.getMainHandItem().isEmpty()) {
            return;
        }
        if (MarionetteManager.handleCommand(player, event.getTarget())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    /** 骨骼软化钻洞期间：无法攻击。 */
    @SubscribeEvent
    public static void onAttackWhileCrawling(AttackEntityEvent event) {
        if (event.getEntity().getData(ModAttachments.BONE_CRAWLING)) {
            event.setCanceled(true);
        }
    }

    /** 骨骼软化钻洞期间：无法破坏方块（左键）。 */
    @SubscribeEvent
    public static void onLeftClickBlockWhileCrawling(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntity().getData(ModAttachments.BONE_CRAWLING)) {
            event.setCanceled(true);
        }
    }

    /** 骨骼软化钻洞期间：无法与方块交互（右键）。 */
    @SubscribeEvent
    public static void onRightClickBlockWhileCrawling(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity().getData(ModAttachments.BONE_CRAWLING)) {
            event.setCanceled(true);
        }
    }
}
