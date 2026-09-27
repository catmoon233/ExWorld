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
 * 扭曲区域同步包（服务端 → 客户端）。
 * <p>
 * 扭曲模式开启期间，服务端周期性地把当前世界内的扭曲区域（封闭屏障 / 隔绝房间）
 * 的边界方块坐标下发给客户端，由 {@link DistortionClientState} 登记后供渲染层
 * 在世界中绘制淡紫色描边线框，让玩家肉眼可见屏障范围。
 *
 * @param positions 需要描边的扭曲区域边界方块
 * @param duration  描边持续时间（tick）
 */
public record DistortionZoneSyncPacket(List<BlockPos> positions, int duration) implements CustomPacketPayload {
    public static final Type<DistortionZoneSyncPacket> TYPE = new Type<>(GuimiMod.id("distortion_zone_sync"));

    public static final StreamCodec<FriendlyByteBuf, DistortionZoneSyncPacket> STREAM_CODEC =
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
                        return new DistortionZoneSyncPacket(positions, duration);
                    }
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** 客户端处理：仅在客户端注册（playToClient），登记描边方块。 */
    public static void handle(DistortionZoneSyncPacket msg, IPayloadContext ctx) {
        ClientPayloadDispatcher.handle(msg, ctx);
    }
}
