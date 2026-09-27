package com.wan.gmmod.common.network.packet;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.content.abilities.SkillManager;
import com.wan.gmmod.content.distortion.DistortionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nullable;

/**
 * 扭曲施放包（客户端 → 服务端）。
 * <p>
 * 玩家处于「扭曲模式」时，左键对目标实体 / 门方块、或右键拖拽出区域后发送本包，
 * 服务端校验能力归属（完整版 = 黑皇帝·序列6 腐化男爵；弱化版 = 门·序列6、
 * 命运之轮·序列6）并执行对应的扭曲效果。
 * <p>
 * {@code type} 取值：
 * <ul>
 *   <li>0 移动反向（实体目标）；</li>
 *   <li>1 攻击转移（实体目标）；</li>
 *   <li>2 弹射物偏转（无需目标）；</li>
 *   <li>3 封闭屏障（目标方块 = 门）；</li>
 *   <li>4 隔绝房间（区域 min/max）；</li>
 *   <li>5 占卜 / 通灵劫持（实体目标 = 玩家）。</li>
 * </ul>
 */
public record DistortionCastPacket(int action, int entityId,
                                   @Nullable BlockPos pos,
                                   @Nullable BlockPos zoneMin, @Nullable BlockPos zoneMax)
        implements CustomPacketPayload {
    public static final Type<DistortionCastPacket> TYPE = new Type<>(GuimiMod.id("distortion_cast"));

    /** 扭曲类型：移动反向 */
    public static final int T_MOVE_INVERT = 0;
    /** 扭曲类型：攻击转移 */
    public static final int T_ATTACK_REDIRECT = 1;
    /** 扭曲类型：弹射物偏转 */
    public static final int T_DEFLECT = 2;
    /** 扭曲类型：封闭屏障 */
    public static final int T_SEAL_DOOR = 3;
    /** 扭曲类型：隔绝房间 */
    public static final int T_ISOLATE = 4;
    /** 扭曲类型：占卜 / 通灵劫持 */
    public static final int T_DIVINATION_HIJACK = 5;

    public static final StreamCodec<FriendlyByteBuf, DistortionCastPacket> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> {
                        buf.writeVarInt(msg.action());
                        buf.writeVarInt(msg.entityId());
                        buf.writeBoolean(msg.pos() != null);
                        if (msg.pos() != null) {
                            buf.writeBlockPos(msg.pos());
                        }
                        boolean hasZone = msg.zoneMin() != null && msg.zoneMax() != null;
                        buf.writeBoolean(hasZone);
                        if (hasZone) {
                            buf.writeBlockPos(msg.zoneMin());
                            buf.writeBlockPos(msg.zoneMax());
                        }
                    },
                    buf -> {
                        int action = buf.readVarInt();
                        int entityId = buf.readVarInt();
                        BlockPos pos = buf.readBoolean() ? buf.readBlockPos() : null;
                        BlockPos zoneMin = null;
                        BlockPos zoneMax = null;
                        if (buf.readBoolean()) {
                            zoneMin = buf.readBlockPos();
                            zoneMax = buf.readBlockPos();
                        }
                        return new DistortionCastPacket(action, entityId, pos, zoneMin, zoneMax);
                    }
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DistortionCastPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) {
                return;
            }
            // 防作弊：必须处于扭曲模式
            if (!DistortionManager.isInMode(sp)) {
                sp.displayClientMessage(Component.translatable(
                        "message.guimi_mod.distortion.not_in_mode"), true);
                return;
            }
            // 能力归属校验：完整版或对应弱化版
            boolean full = SkillManager.isUnlocked(sp, GuimiMod.id("be_distortion"));
            boolean door = SkillManager.isUnlocked(sp, GuimiMod.id("door_distortion"));
            boolean wheel = SkillManager.isUnlocked(sp, GuimiMod.id("whl_distortion"));
            if (!full && !door && !wheel) {
                sp.displayClientMessage(Component.translatable(
                        "message.guimi_mod.skill.locked"), true);
                return;
            }

            ServerLevel level = sp.serverLevel();
            switch (msg.action()) {
                case T_MOVE_INVERT -> {
                    if (!full && !door) {
                        denied(sp);
                        return;
                    }
                    LivingEntity target = living(level, msg.entityId());
                    if (target == null) {
                        noTarget(sp);
                        return;
                    }
                    DistortionManager.applyEntityDistortion(sp, target,
                            DistortionManager.TYPE_MOVE_INVERT, 5);
                    exitMode(sp);
                }
                case T_ATTACK_REDIRECT -> {
                    if (!full && !wheel) {
                        denied(sp);
                        return;
                    }
                    LivingEntity target = living(level, msg.entityId());
                    if (target == null) {
                        noTarget(sp);
                        return;
                    }
                    DistortionManager.applyEntityDistortion(sp, target,
                            DistortionManager.TYPE_ATTACK_REDIRECT, 5);
                    exitMode(sp);
                }
                case T_DEFLECT -> {
                    DistortionManager.startDeflect(sp, 3);
                    sp.displayClientMessage(Component.translatable(
                            "message.guimi_mod.distortion.deflect_on"), true);
                    exitMode(sp);
                }
                case T_SEAL_DOOR -> {
                    if (!full && !door) {
                        denied(sp);
                        return;
                    }
                    BlockPos doorPos = msg.pos();
                    if (doorPos == null) {
                        noTarget(sp);
                        return;
                    }
                    DistortionManager.sealDoor(sp, doorPos);
                    exitMode(sp);
                }
                case T_ISOLATE -> {
                    if (!full && !door) {
                        denied(sp);
                        return;
                    }
                    if (msg.zoneMin() == null || msg.zoneMax() == null) {
                        noTarget(sp);
                        return;
                    }
                    DistortionManager.isolateRegion(sp, msg.zoneMin(), msg.zoneMax());
                    exitMode(sp);
                }
                case T_DIVINATION_HIJACK -> {
                    if (!full && !wheel) {
                        denied(sp);
                        return;
                    }
                    LivingEntity target = living(level, msg.entityId());
                    if (!(target instanceof ServerPlayer victim)) {
                        noTarget(sp);
                        return;
                    }
                    DistortionManager.applyEntityDistortion(sp, victim,
                            DistortionManager.TYPE_DIVINATION_HIJACK, 30);
                    exitMode(sp);
                }
                default -> denied(sp);
            }
        });
    }

    /** 退出扭曲模式并提示成功。 */
    private static void exitMode(ServerPlayer sp) {
        DistortionManager.exitMode(sp);
        sp.displayClientMessage(Component.translatable(
                "message.guimi_mod.distortion.cast"), true);
    }

    private static void denied(ServerPlayer sp) {
        sp.displayClientMessage(Component.translatable(
                "message.guimi_mod.distortion.denied"), true);
    }

    private static void noTarget(ServerPlayer sp) {
        sp.displayClientMessage(Component.translatable(
                "message.guimi_mod.distortion.no_target"), true);
    }

    @Nullable
    private static LivingEntity living(ServerLevel level, int entityId) {
        if (entityId <= 0) {
            return null;
        }
        Entity e = level.getEntity(entityId);
        return e instanceof LivingEntity le ? le : null;
    }
}