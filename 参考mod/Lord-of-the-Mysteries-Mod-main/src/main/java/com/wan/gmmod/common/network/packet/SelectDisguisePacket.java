package com.wan.gmmod.common.network.packet;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.disguise.DisguiseManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 选择变形目标（客户端 → 服务端）。
 * <p>
 * 玩家在变形界面点击怪物图鉴条目时发送；{@code mobId} 为空串表示恢复原样。
 * 服务端在 {@link DisguiseManager#setDisguise} 中完成能力解锁 / 人形白名单 /
 * 图鉴解锁三重校验后写入 {@code DISGUISE_STATE} 附件（自动同步到所有客户端）。
 *
 * @param mobId 目标怪物实体类型 ID 字符串，空串表示恢复原样
 */
public record SelectDisguisePacket(String mobId) implements CustomPacketPayload {
    public static final Type<SelectDisguisePacket> TYPE = new Type<>(GuimiMod.id("select_disguise"));

    public static final StreamCodec<FriendlyByteBuf, SelectDisguisePacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, SelectDisguisePacket::mobId,
                    SelectDisguisePacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SelectDisguisePacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();
            // 服务端校验：仅愚者途径且序列 ≤ 6（无面人）可使用变形库
            if (!"fool".equals(player.getData(com.wan.gmmod.common.capability.ModAttachments.PATHWAY))
                    || player.getData(com.wan.gmmod.common.capability.ModAttachments.SEQUENCE_LEVEL) > 6) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                        "message.guimi_mod.fool_locked_disguise"), true);
                return;
            }
            ResourceLocation id = (msg.mobId() == null || msg.mobId().isEmpty())
                    ? null : ResourceLocation.tryParse(msg.mobId());
            DisguiseManager.setDisguise(player, id);
        });
    }
}
