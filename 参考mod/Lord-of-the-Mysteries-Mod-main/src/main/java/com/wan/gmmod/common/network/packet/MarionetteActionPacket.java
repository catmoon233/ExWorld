package com.wan.gmmod.common.network.packet;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.marionette.MarionetteManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 秘偶操控动作包：客户端 → 服务端。
 * <p>
 * 「共享视野」期间的动作指令：
 * <ul>
 *   <li>{@link #EXIT}：潜行键退出操控；</li>
 *   <li>{@link #ATTACK}：左键 → 秘偶近战攻击（伤害为秘偶基础攻击力），
 *       {@code targetId} 为客户端射线拾取到的目标实体 network id（-1 表示空挥）；</li>
 *   <li>{@link #ABILITY}：右键 → 触发秘偶保留的原有非凡能力（如远程攻击）。</li>
 * </ul>
 */
public record MarionetteActionPacket(int action, int targetId) implements CustomPacketPayload {
    public static final int EXIT = 0;
    public static final int ATTACK = 1;
    public static final int ABILITY = 2;

    public static final Type<MarionetteActionPacket> TYPE =
            new Type<>(GuimiMod.id("marionette_action"));

    public static final StreamCodec<FriendlyByteBuf, MarionetteActionPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, MarionetteActionPacket::action,
                    ByteBufCodecs.INT, MarionetteActionPacket::targetId,
                    MarionetteActionPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MarionetteActionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                MarionetteManager.handleAction(player, packet.action(), packet.targetId());
            }
        });
    }
}
