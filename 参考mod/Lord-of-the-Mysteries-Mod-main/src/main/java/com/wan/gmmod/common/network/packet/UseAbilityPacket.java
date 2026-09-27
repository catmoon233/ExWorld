package com.wan.gmmod.common.network.packet;

import com.wan.gmmod.content.spirituality.SpiritualityManager;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.capability.ModAttachments;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record UseAbilityPacket(int dummy) implements CustomPacketPayload {
    public static final Type<UseAbilityPacket> TYPE = new Type<>(GuimiMod.id("use_ability"));
    public static final StreamCodec<FriendlyByteBuf, UseAbilityPacket> STREAM_CODEC =
            StreamCodec.unit(new UseAbilityPacket(0));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UseAbilityPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
        });
    }
}