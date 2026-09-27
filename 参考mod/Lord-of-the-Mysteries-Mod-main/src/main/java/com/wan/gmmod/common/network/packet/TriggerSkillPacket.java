package com.wan.gmmod.common.network.packet;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.abilities.SkillManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 触发技能栏槽位（客户端 → 服务端）。
 * <p>
 * 快捷键按下时发送，携带槽位索引。服务端在 {@link SkillManager#trigger} 中完成
 * 归属 / 灵性 / 冷却校验并执行能力，防止客户端作弊。
 *
 * @param slot 槽位索引（0 ~ 14）
 */
public record TriggerSkillPacket(int slot) implements CustomPacketPayload {
    public static final Type<TriggerSkillPacket> TYPE = new Type<>(GuimiMod.id("trigger_skill"));

    public static final StreamCodec<FriendlyByteBuf, TriggerSkillPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, TriggerSkillPacket::slot,
                    TriggerSkillPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TriggerSkillPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
        });
    }
}
