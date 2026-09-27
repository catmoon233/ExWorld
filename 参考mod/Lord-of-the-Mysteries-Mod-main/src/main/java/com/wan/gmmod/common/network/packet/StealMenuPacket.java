package com.wan.gmmod.common.network.packet;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.network.ClientPayloadDispatcher;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * 盗火人「隔空盗窃」选择菜单（服务端 → 客户端）。
 * <p>
 * 施放时目标身上有多件物品，服务端下发目标实体 id 与按顺序排列的物品名标签，
 * 客户端据此打开选择界面；玩家点击某行后回发 {@link StealChoicePacket} 完成偷取。
 * 仅用于展示，偷取结果以服务端校验为准。
 */
public record StealMenuPacket(int targetId, List<String> labels) implements CustomPacketPayload {
    public static final Type<StealMenuPacket> TYPE = new Type<>(GuimiMod.id("steal_menu"));

    public static final StreamCodec<FriendlyByteBuf, StealMenuPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, StealMenuPacket::targetId,
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), StealMenuPacket::labels,
                    StealMenuPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(StealMenuPacket msg, IPayloadContext context) {
        ClientPayloadDispatcher.handle(msg, context);
    }
}
