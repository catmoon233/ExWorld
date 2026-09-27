package com.wan.gmmod.common.network.packet;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.common.item.RitualDaggerItem;
import com.wan.gmmod.content.spiritwall.SpiritWallManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 灵性之墙切换包：客户端按 X 键 → 服务端激活/解除灵性之墙。
 */
public record SpiritWallTogglePacket() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SpiritWallTogglePacket> TYPE =
            new CustomPacketPayload.Type<>(GuimiMod.id("spirit_wall_toggle"));

    public static final StreamCodec<FriendlyByteBuf, SpiritWallTogglePacket> STREAM_CODEC =
            StreamCodec.unit(new SpiritWallTogglePacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SpiritWallTogglePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                // 检查是否手持仪式匕首
                boolean holdingDagger = player.getMainHandItem().getItem() instanceof RitualDaggerItem
                        || player.getOffhandItem().getItem() instanceof RitualDaggerItem;

                if (!holdingDagger) {
                    player.displayClientMessage(
                            Component.translatable("message.guimi_mod.need_ritual_dagger"), true);
                    return;
                }

                // 切换灵性之墙状态
                SpiritWallManager.toggle(player);
            }
        });
    }
}
