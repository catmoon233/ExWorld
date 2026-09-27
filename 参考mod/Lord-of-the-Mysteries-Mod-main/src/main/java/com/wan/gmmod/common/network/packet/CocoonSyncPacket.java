package com.wan.gmmod.common.network.packet;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.network.ClientPayloadDispatcher;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 蛛丝蚕茧同步包：服务端 → 客户端。
 * <p>
 * 蚕茧释放时通知周边客户端在对应实体周围渲染半透明蛛丝外壳 + 地面蛛丝圆阵，
 * 被本地护住的玩家还会叠加第一人称蛛丝滤色；火焰破茧时发送本包并标记烧毁，
 * 客户端据此立即切到红色滤网并让外壳透明消退。
 */
public record CocoonSyncPacket(int entityId, int remainingTicks, boolean burning) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CocoonSyncPacket> TYPE =
            new CustomPacketPayload.Type<>(GuimiMod.id("cocoon_sync"));

    public static final StreamCodec<FriendlyByteBuf, CocoonSyncPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, CocoonSyncPacket::entityId,
                    ByteBufCodecs.VAR_INT, CocoonSyncPacket::remainingTicks,
                    ByteBufCodecs.BOOL, CocoonSyncPacket::burning,
                    CocoonSyncPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CocoonSyncPacket packet, IPayloadContext context) {
        ClientPayloadDispatcher.handle(packet, context);
    }
}
