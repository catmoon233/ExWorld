package com.wan.gmmod.common.event;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.common.capability.data.CooldownData;
import com.wan.gmmod.common.network.packet.CocoonSyncPacket;
import com.wan.gmmod.content.abilities.FrostArmorAbility;
import com.wan.gmmod.content.abilities.MirrorSubstituteAbility;
import com.wan.gmmod.content.abilities.PersuadeAbility;
import com.wan.gmmod.content.abilities.SkillManager;
import com.wan.gmmod.content.abilities.WandSubstituteAbility;
import com.wan.gmmod.content.abilities.WeakpointStrikeAbility;
import com.wan.gmmod.content.charm.CharmManager;
import com.wan.gmmod.common.registry.ModItems;
import com.wan.gmmod.content.effects.FallCorruptionEffect;
import com.wan.gmmod.content.witch.WitchPathwayManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 魔女途径（刺客 / 教唆者 / 女巫 / 欢愉魔女）能力相关事件监听：
 * <ul>
 *   <li><b>弱点打击</b>：潜行状态从背后攻击时伤害 ×2，冷却 5 秒；</li>
 *   <li><b>轻盈步伐</b>：免疫摔落伤害；</li>
 *   <li><b>隐形中断</b>：攻击或受伤立即现身；</li>
 *   <li><b>蛛丝蚕茧</b>：非火焰伤害无敌，火焰伤害提前破茧且双倍；</li>
 *   <li><b>冰霜护甲</b>：吸收 30% 伤害；</li>
 *   <li><b>镜子 / 魔杖替身</b>：受到致命伤害时自动触发；</li>
 *   <li><b>说服</b>：与村民交互时施加交易折扣，低概率追加隐藏交易；</li>
 *   <li><b>魅惑驱动</b>：每刻驱动被魅惑生物的 AI（{@link CharmManager}）。</li>
 * </ul>
 */
@EventBusSubscriber(modid = GuimiMod.MODID)
public class WitchAbilityEventSubscriber {
    /** 背刺背后判定阈值：目标朝向与「目标→攻击者」水平向量点积小于此值视为背后。 */
    private static final double BACKSTAB_DOT = -0.2;

    /** 弱点打击：潜行从背后攻击首次伤害 ×2（背刺），冷却 5 秒。 */
    @SubscribeEvent
    public static void onBackstab(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer sp)) {
            return;
        }
        if (!sp.isShiftKeyDown() || !SkillManager.isUnlocked(sp, GuimiMod.id("weakpoint_strike"))) {
            return;
        }
        long now = sp.level().getGameTime();
        if (sp.getData(ModAttachments.BACKSTAB_COOLDOWN_END) > now) {
            return;
        }
        LivingEntity victim = event.getEntity();
        // 水平背后判定：目标朝向 vs 目标→攻击者
        float yaw = victim.getYRot() * ((float) Math.PI / 180F);
        Vec3 look = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
        Vec3 toAttacker = new Vec3(sp.getX() - victim.getX(), 0, sp.getZ() - victim.getZ());
        if (toAttacker.lengthSqr() < 1.0E-4) {
            return;
        }
        double dot = look.dot(toAttacker.normalize());
        if (dot > BACKSTAB_DOT) {
            return;
        }
        event.setAmount(event.getAmount() * WeakpointStrikeAbility.DAMAGE_MULTIPLIER);
        sp.setData(ModAttachments.BACKSTAB_COOLDOWN_END, now + WeakpointStrikeAbility.BACKSTAB_COOLDOWN);
        if (sp.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.CRIT,
                    victim.getX(), victim.getY() + victim.getBbHeight() * 0.6, victim.getZ(),
                    12, 0.3, 0.3, 0.3, 0.2);
            level.playSound(null, victim.blockPosition(),
                    SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 0.8F);
        }
    }

    /** 攻击时中断隐形（立即现身）。 */
    @SubscribeEvent
    public static void onWitchAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            interruptInvisibility(sp);
        }
    }

    /** 轻盈步伐：免疫摔落伤害。 */
    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp
                && SkillManager.isUnlocked(sp, GuimiMod.id("light_footsteps"))) {
            event.setCanceled(true);
        }
    }

    /** 防御性处理：隐形中断 + 蚕茧无敌 / 破茧 + 冰霜护甲减伤（在减伤 / 抵消阶段优先执行）。 */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onWitchDefense(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) {
            return;
        }
        // 受伤中断隐形
        interruptInvisibility(sp);

        // 蛛丝蚕茧：非火焰无敌，火焰提前破茧并造成双倍伤害
        if (WitchPathwayManager.isInCocoon(sp)) {
            if (event.getSource().is(DamageTypeTags.IS_FIRE)) {
                sp.setData(ModAttachments.COCOON_END, 0L);
                event.setAmount(event.getAmount() * 2.0F);
                if (sp.level() instanceof ServerLevel level) {
                    level.sendParticles(ParticleTypes.LAVA,
                            sp.getX(), sp.getY() + 1.0, sp.getZ(), 20, 0.3, 0.6, 0.3, 0.02);
                }
                // 通知周边客户端蚕茧被火打破：外壳透明消退 + 第一人称滤网转红
                PacketDistributor.sendToPlayersTrackingEntityAndSelf(sp,
                        new CocoonSyncPacket(sp.getId(), 30, true));
                sp.displayClientMessage(Component.translatable("message.guimi_mod.cocoon.broken"), true);
            } else {
                event.setCanceled(true);
                return;
            }
        }

        // 冰霜护甲：吸收 30% 伤害
        if (WitchPathwayManager.hasFrostArmor(sp)) {
            event.setAmount(event.getAmount() * (1.0F - FrostArmorAbility.ABSORB_RATIO));
        }
    }

    /** 致命伤自动触发镜子 / 魔杖替身（在减伤之后判定，故用最低优先级）。 */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onFatalDamage(LivingIncomingDamageEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer sp)) {
            return;
        }
        if (event.getAmount() < sp.getHealth()) {
            return;
        }
        // 深渊化克制：堕落状态下替身自动救援失效，无法逃离
        if (FallCorruptionEffect.isCorrupted(sp)) {
            return;
        }
        long now = sp.level().getGameTime();
        // 优先镜子替身（有锚点且冷却就绪）
        if (SkillManager.isUnlocked(sp, GuimiMod.id("mirror_substitute"))
                && !sp.getData(ModAttachments.MIRROR_ANCHOR).isEmpty()
                && SkillManager.cooldownRemaining(sp, GuimiMod.id("mirror_substitute")) <= 0L
                && MirrorSubstituteAbility.teleportToAnchor(sp, true)) {
            rescue(sp, GuimiMod.id("mirror_substitute"), now, 120 * 20);
            event.setCanceled(true);
            return;
        }
        // 后备魔杖替身（有持有绑定魔杖的玩家且冷却就绪）
        if (SkillManager.isUnlocked(sp, GuimiMod.id("wand_substitute"))
                && SkillManager.cooldownRemaining(sp, GuimiMod.id("wand_substitute")) <= 0L
                && WandSubstituteAbility.teleportToWand(sp, true)) {
            rescue(sp, GuimiMod.id("wand_substitute"), now, 120 * 20);
            event.setCanceled(true);
        }
    }

    /** 说服：与村民交互时施加交易折扣，低概率追加一条隐藏（稀有）交易。 */
    @SubscribeEvent
    public static void onVillagerInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof ServerPlayer sp)) {
            return;
        }
        if (!(event.getTarget() instanceof Villager villager)) {
            return;
        }
        if (!SkillManager.isUnlocked(sp, GuimiMod.id("persuade"))) {
            return;
        }
        // 交易折扣：短暂「村庄英雄」显著降低交易价格
        sp.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 200, 1, false, false));
        // 隐藏交易：低概率向该村民追加一条稀有交易
        if (sp.getRandom().nextFloat() < PersuadeAbility.HIDDEN_TRADE_CHANCE && villager.getOffers().size() < 12) {
            villager.getOffers().add(new MerchantOffer(
                    new ItemCost(ModItems.SOYLE.get(), 6),
                    new net.minecraft.world.item.ItemStack(Items.EXPERIENCE_BOTTLE, 3),
                    2, 8, 0.1F));
        }
    }

    /** 每刻驱动被魅惑生物的 AI（中立化 / 跟随 / 误导）。 */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof Mob mob && !mob.level().isClientSide) {
            CharmManager.tickEntity(mob);
        }
    }

    // ===== 辅助 =====

    /** 攻击 / 受伤时中断隐形：清零计时并移除隐身效果。 */
    private static void interruptInvisibility(ServerPlayer sp) {
        boolean active = sp.getData(ModAttachments.WITCH_INVIS_START) > 0L
                || sp.getData(ModAttachments.WITCH_INVIS_END) > 0L;
        if (!active) {
            return;
        }
        sp.setData(ModAttachments.WITCH_INVIS_START, 0L);
        sp.setData(ModAttachments.WITCH_INVIS_END, 0L);
        sp.removeEffect(MobEffects.INVISIBILITY);
        sp.displayClientMessage(Component.translatable("message.guimi_mod.witch_invisibility.interrupted"), true);
    }

    /** 替身救援善后：保留少量血量并写入冷却。 */
    private static void rescue(ServerPlayer sp, ResourceLocation abilityId, long now, int cooldownTicks) {
        sp.setHealth(Math.max(1.0F, sp.getMaxHealth() * 0.5F));
        sp.clearFire();
        CooldownData cd = sp.getData(ModAttachments.SKILL_COOLDOWNS)
                .with(abilityId, now + cooldownTicks, now);
        sp.setData(ModAttachments.SKILL_COOLDOWNS, cd);
    }
}
