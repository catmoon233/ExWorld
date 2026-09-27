package com.wan.gmmod.content.abilities;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.common.network.packet.CocoonSyncPacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 「蛛丝蚕茧」——欢愉魔女（魔女途径 · 序列 6）主动。
 * <p>
 * 用蛛丝包裹自身 5 秒：无敌 + 生命恢复，期间无法移动和攻击。
 * 但火焰伤害会提前打破蚕茧并造成双倍伤害。
 * 消耗 15 灵性，冷却 120 秒。状态维持见 {@code WitchPathwayManager#tickPlayer}，
 * 无敌 / 破茧判定见 {@code WitchAbilityEventSubscriber#onCocoonDamage}。
 */
public class CocoonAbility extends Ability {
    /** 蚕茧持续时间（刻，5 秒） */
    public static final int DURATION = 5 * 20;

    public CocoonAbility() {
        super(GuimiMod.id("cocoon"), 15, 120 * 20, true);
    }

    @Override
    public void onActivate(Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        player.setData(ModAttachments.COCOON_END, level.getGameTime() + DURATION);
        level.sendParticles(ParticleTypes.ITEM_COBWEB,
                player.getX(), player.getY() + 1.0, player.getZ(),
                60, 0.4, 0.9, 0.4, 0.02);
level.playSound(null, player.blockPosition(),
                SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 1.0F, 0.7F);
        player.displayClientMessage(Component.translatable("message.guimi_mod.cocoon.start"), true);
        // 通知周边客户端渲染蛛丝外壳 + 第一人称滤网（剩余时长 = 完整持续时间）
        if (player instanceof ServerPlayer sp) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(sp,
                    new CocoonSyncPacket(sp.getId(), DURATION, false));
        }
    }

    @Override
    public void onDeactivate(Player player) {
        player.setData(ModAttachments.COCOON_END, 0L);
    }
}
