package com.wan.gmmod.content.abilities;

import com.wan.gmmod.content.spirituality.SpiritualityManager;

import com.wan.gmmod.common.capability.ModAttachments;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 正式途径能力的公共工具：效果规格、射线选目标、范围选目标、粒子与消息。
 * <p>
 * 与实验途径中的 {@code ExpFx} 功能等价，但从 {@code content/exp} 包中独立出来，
 * 供正式途径的能力注册使用，避免与实验能力体系耦合。
 */
public final class AbilityFx {

    /** 效果规格：效果 + 等级（持续时间由使用处决定）。 */
    public record Effect(Holder<MobEffect> effect, int amplifier) {}

    private AbilityFx() {}

    /** 快捷构造效果规格。 */
    public static Effect fx(Holder<MobEffect> effect, int amplifier) {
        return new Effect(effect, amplifier);
    }

    /** 为目标批量施加效果。hidden 为 true 时不显示粒子（常驻被动用）。 */
    public static void apply(LivingEntity target, List<Effect> effects, int duration, boolean hidden) {
        for (Effect e : effects) {
            target.addEffect(new MobEffectInstance(e.effect(), duration, e.amplifier(),
                    hidden, !hidden, !hidden));
        }
    }

    /** 视线射线选目标：返回射线路径上的活体（按距离排序，最多 max 个）。 */
    public static List<LivingEntity> rayTargets(ServerPlayer sp, double range, int max) {
        Vec3 eye = sp.getEyePosition();
        Vec3 end = eye.add(sp.getLookAngle().scale(range));
        List<LivingEntity> hits = new ArrayList<>();
        for (LivingEntity e : sp.serverLevel().getEntitiesOfClass(LivingEntity.class,
                sp.getBoundingBox().inflate(range), le -> le != sp && le.isAlive())) {
            if (e.getBoundingBox().inflate(0.35).clip(eye, end).isPresent()) {
                hits.add(e);
            }
        }
        hits.sort(Comparator.comparingDouble(e -> e.distanceToSqr(sp)));
        return hits.size() > max ? new ArrayList<>(hits.subList(0, max)) : hits;
    }

    /** 目标筛选器。 */
    public enum Filter {
        /** 敌对生物（Enemy，或正在锁定该玩家的生物） */
        HOSTILE,
        /** 亡灵生物（治疗反转） */
        UNDEAD,
        /** 友方：其他玩家 + 被玩家驯服的生物 */
        FRIENDLY,
        /** 任意活体 */
        ALL;

        public boolean test(Player self, LivingEntity e) {
            return switch (this) {
                case HOSTILE -> e instanceof Enemy
                        || (e instanceof Mob mob && mob.getTarget() == self);
                case UNDEAD -> e.isInvertedHealAndHarm();
                case FRIENDLY -> e instanceof Player
                        || (e instanceof TamableAnimal tame && tame.getOwner() instanceof Player);
                case ALL -> true;
            };
        }
    }

    /** 范围选目标（不含玩家自身）。 */
    public static List<LivingEntity> around(ServerPlayer sp, double radius, Filter filter) {
        return sp.serverLevel().getEntitiesOfClass(LivingEntity.class,
                sp.getBoundingBox().inflate(radius),
                e -> e != sp && e.isAlive() && filter.test(sp, e));
    }

    /** 在实体位置播放一簇粒子。 */
    public static void burst(ServerLevel level, LivingEntity at, ParticleOptions particle, int count) {
        level.sendParticles(particle, at.getX(), at.getY() + at.getBbHeight() * 0.5, at.getZ(),
                count, 0.3, 0.4, 0.3, 0.02);
    }

    /** 玩家当前位置亮度是否 ≤ 3（阴影类能力的暗处判定）。 */
    public static boolean inDarkness(ServerPlayer sp) {
        return sp.serverLevel().getMaxLocalRawBrightness(sp.blockPosition()) <= 3;
    }

    /** 触发失败时退还灵性（冷却仍会照常记录）。 */
    public static void refund(ServerPlayer sp, int cost) {
        if (cost > 0) {
            SpiritualityManager.set(sp,
                    SpiritualityManager.get(sp) + cost);
        }
    }

    /** 统一的「已发动」提示（HUD 上方小字）。 */
    public static void activated(ServerPlayer sp, String nameKey) {
        sp.displayClientMessage(Component.translatable(
                "message.guimi_mod.exp.activated", Component.translatable(nameKey)), true);
    }

    /** 统一的「没有目标」提示。 */
    public static void noTarget(ServerPlayer sp) {
        sp.displayClientMessage(Component.translatable("message.guimi_mod.exp.no_target"), true);
    }

    /** 常见负面效果列表（治疗 / 净化类能力用）。 */
    private static final List<Holder<MobEffect>> NEGATIVE_EFFECTS = List.of(
            net.minecraft.world.effect.MobEffects.BLINDNESS,
            net.minecraft.world.effect.MobEffects.CONFUSION,
            net.minecraft.world.effect.MobEffects.DARKNESS,
            net.minecraft.world.effect.MobEffects.WEAKNESS,
            net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,
            net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN,
            net.minecraft.world.effect.MobEffects.POISON,
            net.minecraft.world.effect.MobEffects.WITHER,
            net.minecraft.world.effect.MobEffects.HUNGER,
            net.minecraft.world.effect.MobEffects.UNLUCK,
            net.minecraft.world.effect.MobEffects.LEVITATION);

    /** 移除目标身上的常见负面效果。 */
    public static void clearNegative(LivingEntity target) {
        for (Holder<MobEffect> effect : NEGATIVE_EFFECTS) {
            target.removeEffect(effect);
        }
    }

    /** 增减理智值（限幅 0~100）。 */
    public static void addSanity(ServerPlayer sp, int amount) {
        int sanity = sp.getData(ModAttachments.SANITY);
        sp.setData(ModAttachments.SANITY, Math.max(0, Math.min(ModAttachments.MAX_SANITY, sanity + amount)));
    }

    /** 增减污染值（限幅 0~100）。 */
    public static void addPollution(ServerPlayer sp, int amount) {
    }

    public static void whisper(ServerPlayer sp) {
    }
}