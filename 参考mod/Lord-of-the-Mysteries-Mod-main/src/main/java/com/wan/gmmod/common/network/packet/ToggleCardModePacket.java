package com.wan.gmmod.common.network.packet;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.capability.ModAttachments;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 纸牌发射模式切换包（客户端 → 服务端）。
 * <p>
 * 玩家按下切换键（默认 B）时客户端发送本包，服务端翻转
 * {@link ModAttachments#CARD_SCATTER_MODE}（false = 精准单点，true = 散射），
 * 附件自带 sync 会把新模式同步回客户端，供 HUD 在「飞牌」技能图标
 * 右下角绘制当前模式小图标。
 */
public record ToggleCardModePacket() implements CustomPacketPayload {
    public static final Type<ToggleCardModePacket> TYPE = new Type<>(GuimiMod.id("toggle_card_mode"));

    public static final StreamCodec<FriendlyByteBuf, ToggleCardModePacket> STREAM_CODEC =
            StreamCodec.unit(new ToggleCardModePacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ToggleCardModePacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();
            // 服务端校验：仅愚者途径且序列 ≤ 8（小丑）可切换飞牌散射
            if (!"fool".equals(player.getData(ModAttachments.PATHWAY))
                    || player.getData(ModAttachments.SEQUENCE_LEVEL) > 8) {
                player.displayClientMessage(Component.translatable(
                        "message.guimi_mod.fool_locked_card"), true);
                return;
            }
            boolean scatter = !player.getData(ModAttachments.CARD_SCATTER_MODE);
            player.setData(ModAttachments.CARD_SCATTER_MODE, scatter);
            player.displayClientMessage(Component.translatable(
                    scatter ? "message.guimi_mod.card_mode.scatter"
                            : "message.guimi_mod.card_mode.precise"), true);
        });
    }
}
