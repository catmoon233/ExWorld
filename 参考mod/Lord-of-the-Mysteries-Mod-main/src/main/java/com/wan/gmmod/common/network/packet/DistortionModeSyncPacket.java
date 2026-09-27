package com.wan.gmmod.common.network.packet;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.network.ClientPayloadDispatcher;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 扭曲模式状态同步包：服务端 → 客户端。
 * <p>
 * 进入扭曲模式时服务端发送 {@code active = true}，客户端据此激活扭曲 UI /
 * 拦截鼠标选目标；施放完成或退出模式时发送 {@code active = false} 关闭 UI。
 * 客户端同时维护一个与模式窗口等长的本地倒计时，超时自动关闭（服务端权威校验兜底）。
 */
public record DistortionModeSyncPacket(boolean active) implements CustomPacketPayload {
    public static final Type<DistortionModeSyncPacket> TYPE = new Type<>(GuimiMod.id("distortion_mode_sync"));

    public static final StreamCodec<FriendlyByteBuf, DistortionModeSyncPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, DistortionModeSyncPacket::active,
                    DistortionModeSyncPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DistortionModeSyncPacket msg, IPayloadContext ctx) {
        ClientPayloadDispatcher.handle(msg, ctx);
    }
}
