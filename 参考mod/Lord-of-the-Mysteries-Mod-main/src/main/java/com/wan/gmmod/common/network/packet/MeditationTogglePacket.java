package com.wan.gmmod.common.network.packet;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.meditation.MeditationManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 冥想切换包：客户端按 P 键 → 服务端开始/结束冥想。
 */
public record MeditationTogglePacket() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<MeditationTogglePacket> TYPE =
            new CustomPacketPayload.Type<>(GuimiMod.id("meditation_toggle"));

    public static final StreamCodec<FriendlyByteBuf, MeditationTogglePacket> STREAM_CODEC =
            StreamCodec.unit(new MeditationTogglePacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MeditationTogglePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                MeditationManager.toggle(player);
            }
        });
    }
}
