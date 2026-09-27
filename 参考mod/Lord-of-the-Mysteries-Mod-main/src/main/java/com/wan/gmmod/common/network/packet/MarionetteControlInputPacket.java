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
 * 秘偶操控输入包：客户端 → 服务端。
 * <p>
 * 「共享视野」期间客户端每刻捕获玩家的移动输入（WASD / 跳跃）与视角
 * （已被清零，不作用于玩家本体），发送给服务端由
 * {@link MarionetteManager} 转译为秘偶的移动。
 */
public record MarionetteControlInputPacket(float forward, float strafe, boolean jump,
                                           float yRot, float xRot) implements CustomPacketPayload {

    public static final Type<MarionetteControlInputPacket> TYPE =
            new Type<>(GuimiMod.id("marionette_control_input"));

    public static final StreamCodec<FriendlyByteBuf, MarionetteControlInputPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.FLOAT, MarionetteControlInputPacket::forward,
                    ByteBufCodecs.FLOAT, MarionetteControlInputPacket::strafe,
                    ByteBufCodecs.BOOL, MarionetteControlInputPacket::jump,
                    ByteBufCodecs.FLOAT, MarionetteControlInputPacket::yRot,
                    ByteBufCodecs.FLOAT, MarionetteControlInputPacket::xRot,
                    MarionetteControlInputPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MarionetteControlInputPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                MarionetteManager.handleControlInput(player, packet);
            }
        });
    }
}
