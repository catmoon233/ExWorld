package com.wan.gmmod.common.network.packet;

import com.wan.gmmod.content.spirituality.SpiritualityManager;

import com.wan.gmmod.Config;
import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.content.abilities.SkillManager;
import com.wan.gmmod.content.divination.AntiDivination;
import com.wan.gmmod.content.divination.DivinationType;
import com.wan.gmmod.content.divination.PendulumDivination;
import com.wan.gmmod.content.divination.SpiritCommune;
import com.wan.gmmod.content.entities.SpiritBeing;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * 魔镜占卜请求（客户端 → 服务端）。
 * <p>
 * 玩家在 {@code MirrorDivinationScreen} 选择模式后发送，三种模式共享
 * 灵性消耗（{@link Config#MIRROR_SPIRITUALITY_COST}）与冷却
 * （{@link Config#MIRROR_COOLDOWN_SECONDS}）：
 * <ul>
 *   <li>{@code mode} 0 = <b>占卜</b>：视线内实体 → 探测，否则视线方块 → 地理，
 *   否则内省；复用灵摆逻辑但真实结果权重 +20；</li>
 *   <li>{@code mode} 1 = <b>反占卜</b>：视线 8 格内方块 → 区域干扰场，
 *   否则自我反占卜隐藏 Buff（见 {@link AntiDivination}）；</li>
 *   <li>{@code mode} 2 = <b>通灵</b>：8 米内亡灵生物 / 灵体 →
 *   {@link SpiritCommune}，无目标则不扣费；</li>
 *   <li>{@code mode} 3 = <b>隐秘屏障</b>：自身 30 秒强化反占卜，
 *   暗紫迷雾萦绕周身；</li>
 *   <li>{@code mode} 4 = <b>陷阱符文</b>：视线 8 格内方块处埋下暗紫符文，
 *   部署 3 分钟小型干扰场；无目标方块则落在脚下。</li>
 * </ul>
 */
public record MirrorDivinationPacket(int mode) implements CustomPacketPayload {
    public static final Type<MirrorDivinationPacket> TYPE = new Type<>(GuimiMod.id("mirror_divination"));

    /** 占卜模式的视线实体探测距离 */
    private static final double DIVINE_ENTITY_RANGE = 24.0;
    /** 占卜模式的视线方块探测距离 */
    private static final double DIVINE_BLOCK_RANGE = 16.0;
    /** 区域反占卜的视线方块距离 */
    private static final double ANTI_BLOCK_RANGE = 8.0;
    /** 通灵目标距离 */
    private static final double COMMUNE_RANGE = 8.0;
    /** 魔镜占卜的真实 / 清晰结果额外权重（成功率 +20%） */
    private static final int MIRROR_TRUTH_BONUS = 20;

    public static final StreamCodec<FriendlyByteBuf, MirrorDivinationPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, MirrorDivinationPacket::mode,
                    MirrorDivinationPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MirrorDivinationPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) {
                return;
            }
            // 防作弊：必须已解锁魔镜占卜
            if (!SkillManager.isUnlocked(sp, GuimiMod.id("mirror_divination"))) {
                sp.displayClientMessage(Component.translatable("message.guimi_mod.mirror.locked"), true);
                return;
            }
            // 共享冷却与灵性校验
            long now = sp.serverLevel().getGameTime();
            long cooldownEnd = sp.getData(ModAttachments.MIRROR_COOLDOWN_END);
            if (now < cooldownEnd) {
                sp.sendSystemMessage(Component.translatable(
                        "divination.guimi_mod.fail.cooldown", (cooldownEnd - now) / 20 + 1));
                return;
            }
            int cost = Config.MIRROR_SPIRITUALITY_COST.getAsInt();
            if (SpiritualityManager.get(sp) < cost) {
                sp.sendSystemMessage(Component.translatable(
                        "divination.guimi_mod.fail.spirituality", cost));
                return;
            }

            boolean executed = switch (msg.mode()) {
                case 1 -> antiDivine(sp);
                case 2 -> commune(sp);
                case 3 -> barrier(sp);
                case 4 -> trap(sp);
                default -> divine(sp);
            };
            // 成功执行才扣费并开启共享冷却
            if (executed) {
                int v = SpiritualityManager.get(sp);
                SpiritualityManager.set(sp, Math.max(0, v - cost));
                sp.setData(ModAttachments.MIRROR_COOLDOWN_END,
                        now + Config.MIRROR_COOLDOWN_SECONDS.getAsInt() * 20L);
            }
        });
    }

    // ---------------------------------------------------------------------
    // 模式 0：占卜（灵摆逻辑，成功率 +20%）
    // ---------------------------------------------------------------------

    private static boolean divine(ServerPlayer sp) {
        LivingEntity target = lookTarget(sp, DIVINE_ENTITY_RANGE, e -> true);
        if (target != null) {
            return PendulumDivination.perform(sp, DivinationType.ENTITY,
                    target, null, MIRROR_TRUTH_BONUS, true);
        }
        HitResult hit = sp.pick(DIVINE_BLOCK_RANGE, 0.0F, false);
        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
            return PendulumDivination.perform(sp, DivinationType.POSITION,
                    null, blockHit.getBlockPos(), MIRROR_TRUTH_BONUS, true);
        }
        return PendulumDivination.perform(sp, DivinationType.SELF,
                null, null, MIRROR_TRUTH_BONUS, true);
    }

    // ---------------------------------------------------------------------
    // 模式 1：反占卜
    // ---------------------------------------------------------------------

    private static boolean antiDivine(ServerPlayer sp) {
        HitResult hit = sp.pick(ANTI_BLOCK_RANGE, 0.0F, false);
        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
            AntiDivination.deployArea(sp, blockHit.getBlockPos());
        } else {
            AntiDivination.deploySelf(sp);
        }
        return true;
    }

    // ---------------------------------------------------------------------
    // 模式 3：隐秘屏障（30s 强化反占卜）
    // ---------------------------------------------------------------------

    private static boolean barrier(ServerPlayer sp) {
        AntiDivination.deployBarrier(sp);
        return true;
    }

    // ---------------------------------------------------------------------
    // 模式 4：陷阱符文（3 分钟小型干扰场）
    // ---------------------------------------------------------------------

    private static boolean trap(ServerPlayer sp) {
        HitResult hit = sp.pick(ANTI_BLOCK_RANGE, 0.0F, false);
        BlockPos center = hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK
                ? blockHit.getBlockPos() : sp.blockPosition();
        AntiDivination.deployTrap(sp, center);
        return true;
    }

    // ---------------------------------------------------------------------
    // 模式 2：通灵
    // ---------------------------------------------------------------------

    private static boolean commune(ServerPlayer sp) {
        Predicate<LivingEntity> undead = le -> le.isInvertedHealAndHarm() || le instanceof SpiritBeing;
        // 优先取视线目标，其次 8 米内最近的亡灵生物
        LivingEntity target = lookTarget(sp, COMMUNE_RANGE, undead);
        if (target == null) {
            target = nearestUndead(sp, undead);
        }
        if (target == null) {
            sp.displayClientMessage(Component.translatable("message.guimi_mod.commune.no_target"), true);
            return false;
        }
        SpiritCommune.perform(sp, target);
        return true;
    }

    /** 8 米内最近的符合条件的亡灵生物。 */
    @Nullable
    private static LivingEntity nearestUndead(ServerPlayer sp, Predicate<LivingEntity> filter) {
        AABB box = sp.getBoundingBox().inflate(COMMUNE_RANGE);
        List<LivingEntity> candidates = sp.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != sp && e.isAlive() && filter.test(e));
        return candidates.stream()
                .min(Comparator.comparingDouble(sp::distanceToSqr))
                .orElse(null);
    }

    /** 玩家视线方向命中的第一个符合条件的生物。 */
    @Nullable
    private static LivingEntity lookTarget(ServerPlayer sp, double range, Predicate<LivingEntity> filter) {
        Vec3 eye = sp.getEyePosition();
        Vec3 look = sp.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(range));
        AABB box = sp.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);
        Predicate<Entity> predicate = e ->
                e != sp && e instanceof LivingEntity le && le.isAlive() && filter.test(le);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(sp.level(), sp, eye, end, box, predicate);
        return hit != null && hit.getEntity() instanceof LivingEntity le ? le : null;
    }
}
