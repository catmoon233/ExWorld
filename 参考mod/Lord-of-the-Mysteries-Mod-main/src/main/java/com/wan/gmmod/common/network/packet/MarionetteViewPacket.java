package com.wan.gmmod.common.network.packet;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.network.ClientPayloadDispatcher;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 秘偶共享视野包：服务端 → 客户端。
 * <p>
 * 「共享视野」开启 / 关闭时通知客户端：开启时把摄像机绑定到秘偶实体
 * （按 network id 查找），关闭时恢复到玩家本体。
 * 见 {@link MarionetteControlClientState}。
 */
public record MarionetteViewPacket(boolean active, int entityId) implements CustomPacketPayload {

    public static final Type<MarionetteViewPacket> TYPE = new Type<>(GuimiMod.id("marionette_view"));

    public static final StreamCodec<FriendlyByteBuf, MarionetteViewPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, MarionetteViewPacket::active,
                    ByteBufCodecs.VAR_INT, MarionetteViewPacket::entityId,
                    MarionetteViewPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MarionetteViewPacket packet, IPayloadContext context) {
        ClientPayloadDispatcher.handle(packet, context);
    }
}
