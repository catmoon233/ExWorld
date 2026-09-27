package com.wan.gmmod.content.divination;

import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.common.capability.data.InterferenceFieldData;
import com.wan.gmmod.common.registry.ModEffects;
import com.wan.gmmod.content.sequences.Sequences;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;

/**
 * 反占卜逻辑（魔镜「反占卜」模式，全部在服务端执行）。
 * <ul>
 *   <li><b>自我反占卜</b>：给自己施加隐藏 Buff {@code ANTI_DIVINATION}（5 分钟），
 *   他人以自己为目标占卜时结果被干扰；</li>
 *   <li><b>隐秘屏障</b>：30 秒的强化反占卜屏障，暗紫迷雾萦绕周身，期间自身
 *   免疫一切占卜（反占卜 Buff 每刻刷新，无法被驱散）；</li>
 *   <li><b>区域反占卜</b>：在目标坐标部署不可见灵性干扰场（10×10，10 分钟），
 *   占卜目标落在覆盖范围内时结果被干扰；</li>
 *   <li><b>陷阱符文</b>：在地上留下暗紫符文，持续 5 分钟的小型干扰场，
 *   占卜者踏入即遭干扰，符文以暗紫迷雾粒子显形；</li>
 *   <li><b>干扰判定</b>：占卜者序列高于反占卜者 2 级以上 → 减弱（真实率恢复 30%）；
 *   序列 4 以上魔女的反占卜可完全遮蔽半神（序列 4）以下的占卜。</li>
 * </ul>
 */
public final class AntiDivination {
    /** 自我反占卜持续时间（5 分钟） */
    private static final int SELF_DURATION = 5 * 60 * 20;
    /** 隐秘屏障持续时间（30 秒） */
    public static final int BARRIER_DURATION = 30 * 20;
    /** 陷阱符文持续时间（3 分钟） */
    public static final int TRAP_DURATION = 3 * 60 * 20;
    /** 区域干扰场持续时间（10 分钟） */
    private static final int FIELD_DURATION = 10 * 60 * 20;
    /** 减弱判定：占卜者序列比反占卜者强（数字小）至少 2 级 */
    private static final int WEAKEN_GAP = 2;
    /** 半神门槛：序列 4 及以上（数字 ≤ 4）视为半神级 */
    private static final int DEMIGOD_LEVEL = 4;
    /** 暗紫迷雾的粒子颜色（暗紫） */
    private static final org.joml.Vector3f PURPLE = new org.joml.Vector3f(0.36F, 0.18F, 0.55F);

    private AntiDivination() {
    }

    /** 占卜受到的干扰程度。 */
    public enum Interference {
        /** 无干扰，正常占卜 */
        NONE,
        /** 干扰减弱：真实信息概率恢复 30% */
        WEAKENED,
        /** 完全干扰：真实信息概率降为 0 */
        BLOCKED
    }

    // ---------------------------------------------------------------------
    // 部署
    // ---------------------------------------------------------------------

    /** 自我反占卜：施加 5 分钟隐藏 Buff，镜面闪过水银光泽。 */
    public static void deploySelf(ServerPlayer sp) {
        sp.addEffect(new MobEffectInstance(ModEffects.ANTI_DIVINATION,
                SELF_DURATION, 0, true, false, false));
        playMercurySheen(sp.serverLevel(), sp.blockPosition());
        sp.displayClientMessage(Component.translatable("message.guimi_mod.mirror.anti_self"), true);
    }

    /** 隐秘屏障：30 秒强化反占卜，暗紫迷雾萦绕周身。 */
    public static void deployBarrier(ServerPlayer sp) {
        applyBarrier(sp, BARRIER_DURATION);
        playPurpleHaze(sp.serverLevel(), sp.blockPosition(), 40);
        sp.displayClientMessage(Component.translatable("message.guimi_mod.mirror.anti_barrier"), true);
    }

    /** 施加隐秘屏障反占卜 Buff。 */
    public static void applyBarrier(ServerPlayer sp, int durationTicks) {
        sp.addEffect(new MobEffectInstance(ModEffects.ANTI_DIVINATION,
                durationTicks, 0, false, false, false));
    }

    /** 陷阱符文：在目标坐标部署 3 分钟小型干扰场，并炸出暗紫符文迷雾。 */
    public static void deployTrap(ServerPlayer sp, BlockPos center) {
        ServerLevel level = sp.serverLevel();
        long now = level.getGameTime();
        InterferenceFieldData data = level.getData(ModAttachments.INTERFERENCE_FIELDS);
        data.purgeExpired(now);
        boolean witch = Sequences.Pathway.WITCH.getKey().equals(sp.getData(ModAttachments.PATHWAY));
        data.add(center, now + TRAP_DURATION, sp.getData(ModAttachments.SEQUENCE_LEVEL), witch);
        playPurpleHaze(level, center, 24);
        sp.displayClientMessage(Component.translatable("message.guimi_mod.mirror.anti_trap"), true);
    }

    /** 区域反占卜：在目标坐标部署 10 分钟灵性干扰场。 */
    public static void deployArea(ServerPlayer sp, BlockPos center) {
        ServerLevel level = sp.serverLevel();
        long now = level.getGameTime();
        InterferenceFieldData data = level.getData(ModAttachments.INTERFERENCE_FIELDS);
        data.purgeExpired(now);
        boolean witch = Sequences.Pathway.WITCH.getKey().equals(sp.getData(ModAttachments.PATHWAY));
        data.add(center, now + FIELD_DURATION, sp.getData(ModAttachments.SEQUENCE_LEVEL), witch);
        playMercurySheen(level, center);
        sp.displayClientMessage(Component.translatable("message.guimi_mod.mirror.anti_area"), true);
    }

    /** 镜面水银光泽：白色光尘粒子 + 紫水晶轻响。 */
    private static void playMercurySheen(ServerLevel level, BlockPos pos) {
        level.sendParticles(ParticleTypes.END_ROD,
                pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 24, 0.5, 0.7, 0.5, 0.02);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.9F, 1.6F);
    }

    /** 暗紫迷雾：暗紫灰尘 + 龙息紫雾笼罩目标，并伴随低沉的符文嗡鸣。 */
    private static void playPurpleHaze(ServerLevel level, BlockPos pos, int dustCount) {
        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 0.6;
        double cz = pos.getZ() + 0.5;
        level.sendParticles(new DustParticleOptions(PURPLE, 1.2F),
                cx, cy, cz, dustCount, 0.6, 0.8, 0.6, 0.01);
        level.sendParticles(ParticleTypes.DRAGON_BREATH,
                cx, cy, cz, dustCount / 3, 0.4, 0.5, 0.4, 0.0);
        level.playSound(null, pos, SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 0.5F, 0.4F);
    }

    // ---------------------------------------------------------------------
    // 干扰判定（由 PendulumDivination 在探测 / 地理占卜时调用）
    // ---------------------------------------------------------------------

    /**
     * 判定一次占卜是否被反占卜干扰。
     *
     * @param diviner 占卜者
     * @param target  占卜目标实体（探测占卜时非空）
     * @param pos     占卜目标坐标（地理占卜时非空）
     */
    public static Interference check(ServerPlayer diviner,
                                     @Nullable LivingEntity target, @Nullable BlockPos pos) {
        ServerLevel level = diviner.serverLevel();
        long now = level.getGameTime();

        int protectorSeq = -1;
        boolean protectorWitch = false;

        // 1. 目标玩家自身携带反占卜 Buff
        if (target instanceof ServerPlayer tp && tp.hasEffect(ModEffects.ANTI_DIVINATION)) {
            protectorSeq = tp.getData(ModAttachments.SEQUENCE_LEVEL);
            protectorWitch = Sequences.Pathway.WITCH.getKey().equals(tp.getData(ModAttachments.PATHWAY));
        } else {
            // 2. 目标实体所在位置 / 目标坐标被干扰场覆盖
            BlockPos checkPos = target != null ? target.blockPosition() : pos;
            if (checkPos == null) {
                return Interference.NONE;
            }
            InterferenceFieldData.Field field =
                    level.getData(ModAttachments.INTERFERENCE_FIELDS).covering(checkPos, now);
            if (field == null) {
                return Interference.NONE;
            }
            protectorSeq = field.ownerSeq();
            protectorWitch = field.witch();
        }

        int divinerSeq = diviner.getData(ModAttachments.SEQUENCE_LEVEL);

        // 序列 4 以上魔女的反占卜：完全遮蔽半神以下（序列 5~9 或未就职）的占卜
        if (protectorWitch && protectorSeq > 0 && protectorSeq <= DEMIGOD_LEVEL
                && (divinerSeq <= 0 || divinerSeq > DEMIGOD_LEVEL)) {
            return Interference.BLOCKED;
        }
        // 占卜者序列高于反占卜者 2 级以上 → 干扰减弱
        if (divinerSeq > 0 && (protectorSeq <= 0 || protectorSeq - divinerSeq >= WEAKEN_GAP)) {
            return Interference.WEAKENED;
        }
        return Interference.BLOCKED;
    }
}
