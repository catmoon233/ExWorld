package com.wan.gmmod.common.network.packet;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.network.ClientPayloadDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * 方块高亮网络包（服务端 → 客户端）。
 * <p>
 * 地理占卜命中「清晰感知」时，服务端扫描周围的矿石 / 宝箱，收集其坐标后
 * 通过本包发送给占卜的玩家。客户端收到后登记到 {@link BlockHighlightClientState}，
 * 由渲染层在世界中对这些方块绘制持续描边线框，持续 {@code duration} tick。
 *
 * @param positions 需要高亮的方块坐标
 * @param duration  持续时间（tick）
 */
public record HighlightBlocksPacket(List<BlockPos> positions, int duration) implements CustomPacketPayload {
    public static final Type<HighlightBlocksPacket> TYPE = new Type<>(GuimiMod.id("highlight_blocks"));

    public static final StreamCodec<FriendlyByteBuf, HighlightBlocksPacket> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> {
                        buf.writeVarInt(msg.duration());
                        buf.writeVarInt(msg.positions().size());
                        for (BlockPos pos : msg.positions()) {
                            buf.writeBlockPos(pos);
                        }
                    },
                    buf -> {
                        int duration = buf.readVarInt();
                        int size = buf.readVarInt();
                        List<BlockPos> positions = new ArrayList<>(size);
                        for (int i = 0; i < size; i++) {
                            positions.add(buf.readBlockPos());
                        }
                        return new HighlightBlocksPacket(positions, duration);
                    }
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * 客户端处理：仅在客户端注册（playToClient），这里的逻辑只会在客户端执行。
     */
    public static void handle(HighlightBlocksPacket msg, IPayloadContext ctx) {
        ClientPayloadDispatcher.handle(msg, ctx);
    }
}
