package com.wan.gmmod.common.network.packet;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.item.SilenceGunItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 寂灭开火包：客户端左键（空挥）→ 服务端执行射击。
 * <p>
 * 左键点击实体 / 方块由服务端事件（AttackEntityEvent / LeftClickBlock）直接处理，
 * 左键空挥仅客户端可见，因此需要通过本包通知服务端。
 */
public record SilenceGunFirePacket() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SilenceGunFirePacket> TYPE =
            new CustomPacketPayload.Type<>(GuimiMod.id("silence_gun_fire"));

    public static final StreamCodec<FriendlyByteBuf, SilenceGunFirePacket> STREAM_CODEC =
            StreamCodec.unit(new SilenceGunFirePacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SilenceGunFirePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player
                    && player.getMainHandItem().getItem() instanceof SilenceGunItem gun) {
                gun.fire(player);
            }
        });
    }
}
