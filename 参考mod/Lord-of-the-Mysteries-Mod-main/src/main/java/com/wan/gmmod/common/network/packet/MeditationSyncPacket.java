package com.wan.gmmod.common.network.packet;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.network.ClientPayloadDispatcher;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 冥想状态同步包：服务端 → 客户端。
 * <p>
 * 冥想开始/结束时通知客户端，客户端据此锁定移动输入并固定视角，
 * 实现"站立闭眼、无法移动、视角固定"的冥想姿态。
 */
public record MeditationSyncPacket(boolean meditating) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<MeditationSyncPacket> TYPE =
            new CustomPacketPayload.Type<>(GuimiMod.id("meditation_sync"));

    public static final StreamCodec<FriendlyByteBuf, MeditationSyncPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, MeditationSyncPacket::meditating,
                    MeditationSyncPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MeditationSyncPacket packet, IPayloadContext context) {
        ClientPayloadDispatcher.handle(packet, context);
    }
}
