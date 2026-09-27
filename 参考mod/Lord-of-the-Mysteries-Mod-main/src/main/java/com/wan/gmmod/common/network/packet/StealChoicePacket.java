package com.wan.gmmod.common.network.packet;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.exp.ExpFx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 盗火人「隔空盗窃」选择包（客户端 → 服务端）。
 * <p>
 * 玩家在选择菜单中点击目标身上的某一物品后回发；服务端校验目标仍存活且在 50 米内，
 * 并按索引偷取该物品放入施放者背包。
 *
 * @param targetId 目标实体 id
 * @param index    所选物品在目标物品槽列表中的下标（与菜单显示顺序一致）
 */
public record StealChoicePacket(int targetId, int index) implements CustomPacketPayload {
    public static final Type<StealChoicePacket> TYPE = new Type<>(GuimiMod.id("steal_choice"));

    public static final StreamCodec<FriendlyByteBuf, StealChoicePacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, StealChoicePacket::targetId,
                    ByteBufCodecs.VAR_INT, StealChoicePacket::index,
                    StealChoicePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(StealChoicePacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) return;
            ServerLevel level = sp.serverLevel();
            if (level.getEntity(msg.targetId()) instanceof LivingEntity target
                    && target.isAlive() && sp.distanceToSqr(target) <= 2500) {
                ItemStack loot = ExpFx.stealFromIndex(target, msg.index());
                if (!loot.isEmpty()) {
                    sp.getInventory().placeItemBackInInventory(loot);
                    sp.displayClientMessage(Component.translatable("message.guimi_mod.exp.steal_item",
                            loot.getHoverName()), true);
                    ExpFx.burst(level, target, ParticleTypes.SOUL, 10);
                }
            }
        });
    }
}