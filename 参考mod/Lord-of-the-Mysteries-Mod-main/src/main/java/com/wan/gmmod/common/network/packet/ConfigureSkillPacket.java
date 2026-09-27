package com.wan.gmmod.common.network.packet;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.abilities.SkillManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 配置技能栏槽位（客户端 → 服务端）。
 * <p>
 * 在技能配置界面拖放 / 点击能力到槽位时发送。{@code abilityId} 为空串表示清空该槽。
 * 服务端在 {@link SkillManager#configure} 中校验该能力是否属于玩家当前途径与序列（防作弊）。
 *
 * @param slot      槽位索引（0 ~ 14）
 * @param abilityId 能力 ID 字符串，空串表示清空
 */
public record ConfigureSkillPacket(int slot, String abilityId) implements CustomPacketPayload {
    public static final Type<ConfigureSkillPacket> TYPE = new Type<>(GuimiMod.id("configure_skill"));

    public static final StreamCodec<FriendlyByteBuf, ConfigureSkillPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, ConfigureSkillPacket::slot,
                    ByteBufCodecs.STRING_UTF8, ConfigureSkillPacket::abilityId,
                    ConfigureSkillPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ConfigureSkillPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ResourceLocation id = (msg.abilityId() == null || msg.abilityId().isEmpty())
                    ? null : ResourceLocation.tryParse(msg.abilityId());
            SkillManager.configure(ctx.player(), msg.slot(), id);
        });
    }
}
