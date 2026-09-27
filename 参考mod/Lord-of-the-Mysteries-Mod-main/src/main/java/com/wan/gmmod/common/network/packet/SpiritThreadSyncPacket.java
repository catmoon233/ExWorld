package com.wan.gmmod.common.network.packet;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.network.ClientPayloadDispatcher;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 灵体之线同步包：服务端 → 客户端。
 * <p>
 * 秘偶大师存在秘偶 / 灵体之线操控目标时周期性下发（约每 10 刻），
 * 携带两者的 network id（-1 表示无）与挣扎状态；客户端存入
 * {@link SpiritThreadClientState}，供灵体之线视野下的
 * {@code SpiritThreadRenderer} 渲染连接玩家与目标的线纹理
 * （挣扎时线变红并剧烈抖动）。长时间未收到更新时客户端自动清除。
 */
public record SpiritThreadSyncPacket(int marionetteId, int targetId, boolean struggling)
        implements CustomPacketPayload {

    public static final Type<SpiritThreadSyncPacket> TYPE = new Type<>(GuimiMod.id("spirit_thread_sync"));

    public static final StreamCodec<FriendlyByteBuf, SpiritThreadSyncPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, SpiritThreadSyncPacket::marionetteId,
                    ByteBufCodecs.VAR_INT, SpiritThreadSyncPacket::targetId,
                    ByteBufCodecs.BOOL, SpiritThreadSyncPacket::struggling,
                    SpiritThreadSyncPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SpiritThreadSyncPacket packet, IPayloadContext context) {
        ClientPayloadDispatcher.handle(packet, context);
    }
}
