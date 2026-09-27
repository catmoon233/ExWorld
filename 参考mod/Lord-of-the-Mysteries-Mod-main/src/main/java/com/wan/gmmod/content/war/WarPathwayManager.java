package com.wan.gmmod.content.war;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.common.item.FlameWeaponItem;
import com.wan.gmmod.common.registry.ModItems;
import com.wan.gmmod.content.abilities.SkillManager;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * 战争之红途径持续状态维护器（服务端，由 {@code GameEventSubscriber#onPlayerTick} 每刻驱动）。
 * <ul>
 *   <li><b>弱点洞察</b>：潜行观察进度（{@link InsightManager}）；</li>
 *   <li><b>痕迹追踪</b>：每秒记录周围生物足迹（{@link TrackManager}）；</li>
 *   <li><b>直觉预警</b>：背后 180°（升级 360°）威胁检测 + 心跳音效 + 被跟踪感知；</li>
 *   <li><b>灵视强化</b>：开启灵视时向本人发送周围生物的以太体彩色粒子；</li>
 *   <li><b>火焰护甲 / 火焰体质</b>：持续粒子与到期清理；</li>
 *   <li><b>火焰武器</b>：到期自动从背包移除临时武器；</li>
 *   <li><b>凝聚火球</b>：物品与蓄力附件双向同步（丢弃即散、超窗消散、防囤积）；</li>
 *   <li><b>注火</b>：待爆记录到时后在目标体内引爆（+6 伤害）。</li>
 * </ul>
 */
public final class WarPathwayManager {
    /** 直觉预警检测半径。 */
    private static final double DANGER_RANGE = 8.0;
    /** 被跟踪感知半径（危险直觉升级）。 */
    private static final double STALK_RANGE = 24.0;
    /** 注火体内爆炸延时：3 秒，额外伤害 6。 */
    public static final int INJECTION_FUSE = 3 * 20;
    public static final float INJECTION_DAMAGE = 6.0F;

    /** 一条注火待爆记录。 */
    private record PendingInjection(UUID ownerId, UUID victimId, long explodeTime) {
    }

    private static final List<PendingInjection> INJECTIONS = new ArrayList<>();

    private WarPathwayManager() {
    }

    public static void tickPlayer(ServerPlayer sp) {
        if (!(sp.level() instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();

        // 弱点洞察：潜行观察进度
        if (SkillManager.isUnlocked(sp, GuimiMod.id("weakness_insight"))) {
            InsightManager.tick(sp);
        }
        // 痕迹追踪：每秒采样一次周围生物足迹
        if (sp.tickCount % TrackManager.SAMPLE_INTERVAL == 0
                && SkillManager.isUnlocked(sp, GuimiMod.id("trace_tracking"))) {
            TrackManager.record(sp);
        }
        // 直觉预警（每 10 刻检测一次，降低开销）
        if (sp.tickCount % 10 == 0 && SkillManager.isUnlocked(sp, GuimiMod.id("danger_sense"))) {
            tickDangerSense(sp, level, now);
        }
        // 灵视强化：开启灵视时看到周围生物的以太体颜色（仅本人可见）
        if (sp.tickCount % 10 == 5
                && sp.getData(ModAttachments.SPIRIT_VISION)
                && SkillManager.isUnlocked(sp, GuimiMod.id("spirit_vision_enhance"))) {
            tickEtherSight(sp, level);
        }
        // 火焰护甲：持续火焰粒子
        if (sp.getData(ModAttachments.FLAME_ARMOR_END) > now && now % 10 == 0) {
            level.sendParticles(ParticleTypes.FLAME,
                    sp.getX(), sp.getY() + 1.0, sp.getZ(), 4, 0.4, 0.8, 0.4, 0.01);
        }
        // 火焰体质（火焰形态）：密集粒子 + 到期清理
        long fireFormEnd = sp.getData(ModAttachments.FIRE_FORM_END);
        if (fireFormEnd > 0L) {
            if (now >= fireFormEnd) {
                sp.setData(ModAttachments.FIRE_FORM_END, 0L);
                sp.displayClientMessage(Component.translatable("message.guimi_mod.fire_body.end"), true);
            } else if (now % 2 == 0) {
                level.sendParticles(ParticleTypes.FLAME,
                        sp.getX(), sp.getY() + 1.0, sp.getZ(), 8, 0.3, 0.8, 0.3, 0.02);
            }
        }
        // 火焰武器到期：从背包移除临时武器
        long weaponEnd = sp.getData(ModAttachments.FLAME_WEAPON_END);
        if (weaponEnd > 0L && now >= weaponEnd) {
            sp.setData(ModAttachments.FLAME_WEAPON_END, 0L);
            removeFlameWeapons(sp);
            sp.displayClientMessage(Component.translatable("message.guimi_mod.flame_weapon.expired"), true);
        }
        // 凝聚火球：物品与蓄力附件同步（每 10 刻）
        if (sp.tickCount % 10 == 3) {
            tickChargedOrbs(sp, now);
        }
        // 注火：到时引爆（仅由记录持有者驱动，避免重复结算）
        tickInjections(sp, level, now);
    }

    // ===== 直觉预警 =====

    private static void tickDangerSense(ServerPlayer sp, ServerLevel level, long now) {
        boolean advanced = SkillManager.isUnlocked(sp, GuimiMod.id("danger_sense_advanced"));
        Vec3 look = sp.getViewVector(1.0F);
        boolean danger = false;
        for (Mob mob : level.getEntitiesOfClass(Mob.class,
                sp.getBoundingBox().inflate(DANGER_RANGE),
                m -> m.isAlive() && (m.getTarget() == sp || m.getDeltaMovement().lengthSqr() > 1.0E-4))) {
            Vec3 toMob = mob.position().subtract(sp.position());
            if (toMob.lengthSqr() < 1.0E-4) {
                continue;
            }
            // 基础版仅感知背后 180°（视线方向点积 < 0），升级版 360°
            if (advanced || look.dot(toMob.normalize()) < 0.0) {
                danger = true;
                break;
            }
        }
        if (danger != sp.getData(ModAttachments.DANGER_SENSE)) {
            sp.setData(ModAttachments.DANGER_SENSE, danger);
        }
        // 心跳音效（仅本人可闻，每 0.5 秒一次）
        if (danger) {
            sp.playNotifySound(SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 0.7F, 1.2F);
        }
        // 危险直觉升级：感知远处正在跟踪自己的生物
        if (advanced && now % 100 == 0) {
            for (Mob stalker : level.getEntitiesOfClass(Mob.class,
                    sp.getBoundingBox().inflate(STALK_RANGE),
                    m -> m.isAlive() && m.getTarget() == sp && m.distanceToSqr(sp) > DANGER_RANGE * DANGER_RANGE)) {
                sp.displayClientMessage(Component.translatable("message.guimi_mod.danger_sense.stalked",
                        stalker.getDisplayName()), true);
                break;
            }
        }
    }

    // ===== 灵视强化：以太体颜色 =====

    /** 以太体半径。 */
    private static final double ETHER_RANGE = 16.0;

    /**
     * 向本人（且仅本人）发送周围生物的以太体彩色粒子：
     * 红 = 受伤（生命低于一半），绿 = 中毒，金 = 非凡者玩家，淡蓝 = 常态。
     */
    private static void tickEtherSight(ServerPlayer sp, ServerLevel level) {
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class,
                sp.getBoundingBox().inflate(ETHER_RANGE), e -> e != sp && e.isAlive())) {
            Vector3f color;
            if (living instanceof Player p && p.getData(ModAttachments.SEQUENCE_LEVEL) > 0) {
                color = new Vector3f(1.0F, 0.84F, 0.1F);   // 金：非凡者
            } else if (living.hasEffect(MobEffects.POISON)) {
                color = new Vector3f(0.3F, 0.9F, 0.2F);    // 绿：中毒
            } else if (living.getHealth() < living.getMaxHealth() * 0.5F) {
                color = new Vector3f(0.95F, 0.15F, 0.1F);  // 红：重伤
            } else {
                color = new Vector3f(0.6F, 0.8F, 1.0F);    // 淡蓝：常态
            }
            level.sendParticles(sp, new DustParticleOptions(color, 1.0F), true,
                    living.getX(), living.getY() + living.getBbHeight() * 0.6, living.getZ(),
                    4, living.getBbWidth() * 0.5, living.getBbHeight() * 0.4, living.getBbWidth() * 0.5, 0.0);
        }
    }

    // ===== 注火 =====

    /** 登记一条注火待爆记录（近战命中时由事件订阅器调用）。 */
    public static void addInjection(ServerPlayer owner, LivingEntity victim, long now) {
        INJECTIONS.add(new PendingInjection(owner.getUUID(), victim.getUUID(), now + INJECTION_FUSE));
    }

    private static void tickInjections(ServerPlayer sp, ServerLevel level, long now) {
        Iterator<PendingInjection> it = INJECTIONS.iterator();
        while (it.hasNext()) {
            PendingInjection inj = it.next();
            if (!inj.ownerId().equals(sp.getUUID())) {
                continue;
            }
            if (now < inj.explodeTime()) {
                continue;
            }
            it.remove();
            if (level.getEntity(inj.victimId()) instanceof LivingEntity victim && victim.isAlive()) {
                victim.hurt(level.damageSources().indirectMagic(sp, sp), INJECTION_DAMAGE);
                victim.igniteForSeconds(3);
                level.sendParticles(ParticleTypes.EXPLOSION,
                        victim.getX(), victim.getY() + victim.getBbHeight() * 0.5, victim.getZ(),
                        1, 0, 0, 0, 0);
                level.sendParticles(ParticleTypes.FLAME,
                        victim.getX(), victim.getY() + victim.getBbHeight() * 0.5, victim.getZ(),
                        20, 0.3, 0.4, 0.3, 0.05);
                level.playSound(null, victim.blockPosition(),
                        SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.8F, 1.5F);
            }
        }
    }

    // ===== 辅助 =====

    /** 火焰体质（火焰形态）当前是否生效。 */
    public static boolean isFireForm(ServerPlayer sp) {
        return sp.getData(ModAttachments.FIRE_FORM_END) > sp.level().getGameTime();
    }

    /** 火焰护甲当前是否生效。 */
    public static boolean hasFlameArmor(ServerPlayer sp) {
        return sp.getData(ModAttachments.FLAME_ARMOR_END) > sp.level().getGameTime();
    }

    /** 从背包移除全部临时火焰武器。 */
    private static void removeFlameWeapons(ServerPlayer sp) {
        for (int i = 0; i < sp.getInventory().getContainerSize(); i++) {
            ItemStack stack = sp.getInventory().getItem(i);
            if (stack.getItem() instanceof FlameWeaponItem) {
                sp.getInventory().setItem(i, ItemStack.EMPTY);
            }
        }
    }

    // ===== 凝聚火球（火球术 / 巨大火球） =====

    /**
     * 开始凝聚时把凝聚火球握入主手（手臂上即显示凝聚纹理）。
     * 原主手物品手动转移到空槽（先背包区再热栏非选中格），找不到空槽则掉落；
     * 不能用 {@code inventory.add}——它可能把副本合并进主手槽自身，随后被覆盖丢失。
     */
    public static void giveChargedOrb(ServerPlayer sp, boolean giant) {
        ItemStack orb = new ItemStack(giant
                ? ModItems.GIANT_FLAME_ORB_ITEM.get() : ModItems.FLAME_ORB_ITEM.get());
        ItemStack main = sp.getMainHandItem();
        if (!main.isEmpty()) {
            int empty = -1;
            for (int i = 9; i < 36 && empty < 0; i++) {
                if (sp.getInventory().getItem(i).isEmpty()) {
                    empty = i;
                }
            }
            for (int i = 0; i < 9 && empty < 0; i++) {
                if (i != sp.getInventory().selected && sp.getInventory().getItem(i).isEmpty()) {
                    empty = i;
                }
            }
            if (empty >= 0) {
                sp.getInventory().setItem(empty, main.copy());
            } else {
                sp.drop(main.copy(), false);
            }
        }
        sp.setItemInHand(InteractionHand.MAIN_HAND, orb);
    }

    /** 从背包移除全部对应档位的凝聚火球物品。 */
    public static void removeChargedOrbs(ServerPlayer sp, boolean giant) {
        Item target = giant ? ModItems.GIANT_FLAME_ORB_ITEM.get() : ModItems.FLAME_ORB_ITEM.get();
        for (int i = 0; i < sp.getInventory().getContainerSize(); i++) {
            if (sp.getInventory().getItem(i).is(target)) {
                sp.getInventory().setItem(i, ItemStack.EMPTY);
            }
        }
    }

    /** 背包中是否还持有对应档位的凝聚火球。 */
    private static boolean hasChargedOrb(ServerPlayer sp, boolean giant) {
        Item target = giant ? ModItems.GIANT_FLAME_ORB_ITEM.get() : ModItems.FLAME_ORB_ITEM.get();
        for (int i = 0; i < sp.getInventory().getContainerSize(); i++) {
            if (sp.getInventory().getItem(i).is(target)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 凝聚火球与蓄力附件双向同步：
     * <ul>
     *   <li>无蓄力却持有物品 → 清除残留（防捡回囤积）；</li>
     *   <li>蓄力中物品被丢弃 → 蓄力散逸取消；</li>
     *   <li>巨大火球超过蓄力窗口上限 → 凝聚消散并提示。</li>
     * </ul>
     */
    private static void tickChargedOrbs(ServerPlayer sp, long now) {
        // 火球术
        long fbStart = sp.getData(ModAttachments.FIREBALL_CHARGE_START);
        if (fbStart == 0L) {
            removeChargedOrbs(sp, false);
        } else if (!hasChargedOrb(sp, false)) {
            sp.setData(ModAttachments.FIREBALL_CHARGE_START, 0L);
        }
        // 巨大火球
        long gStart = sp.getData(ModAttachments.GIANT_FIREBALL_CHARGE_START);
        if (gStart == 0L) {
            removeChargedOrbs(sp, true);
        } else if (now - gStart > GiantFireballAbility.maxChargeTicks(sp)) {
            sp.setData(ModAttachments.GIANT_FIREBALL_CHARGE_START, 0L);
            removeChargedOrbs(sp, true);
            sp.displayClientMessage(Component.translatable("message.guimi_mod.giant_fireball.dissipated"), true);
        } else if (!hasChargedOrb(sp, true)) {
            sp.setData(ModAttachments.GIANT_FIREBALL_CHARGE_START, 0L);
        }
    }
}
