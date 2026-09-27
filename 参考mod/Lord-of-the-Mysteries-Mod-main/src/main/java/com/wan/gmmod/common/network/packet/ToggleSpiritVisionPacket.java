package com.wan.gmmod.common.network.packet;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.capability.ModAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 灵视切换网络包（客户端 → 服务端）。
 * <p>
 * 玩家按下灵视键（V）时客户端发送本包，服务端翻转
 * {@link ModAttachments#SPIRIT_VISION} 开关：开启时授予夜视效果，
 * 关闭时移除夜视。开关状态通过附件同步回客户端，供实体渲染器判断
 * 是否渲染灵体（{@code SpiritBeing}）。
 * <p>
 * 夜视效果的持续刷新由 {@code GameEventSubscriber.onPlayerTick} 负责，
 * 本包仅在切换瞬间应用 / 移除。
 */
public record ToggleSpiritVisionPacket() implements CustomPacketPayload {
    public static final Type<ToggleSpiritVisionPacket> TYPE = new Type<>(GuimiMod.id("toggle_spirit_vision"));

    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, ToggleSpiritVisionPacket> STREAM_CODEC =
            StreamCodec.unit(new ToggleSpiritVisionPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ToggleSpiritVisionPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();
            if (!com.wan.gmmod.content.vision.SpiritVision.allowed(player)) {
                com.wan.gmmod.content.vision.SpiritVision.turnOff(player);
                return;
            }
            boolean enabled = !Boolean.TRUE.equals(player.getData(ModAttachments.SPIRIT_VISION));
            player.setData(ModAttachments.SPIRIT_VISION, enabled);
            if (enabled) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, false, false, false));
            } else {
                player.removeEffect(MobEffects.NIGHT_VISION);
            }
        });
    }
}
