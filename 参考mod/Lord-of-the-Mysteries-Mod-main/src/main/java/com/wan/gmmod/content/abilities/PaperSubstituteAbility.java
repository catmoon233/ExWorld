package com.wan.gmmod.content.abilities;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.effects.FallCorruptionEffect;
import com.wan.gmmod.content.entities.PaperFigurineEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/**
 * 「纸人替身」——魔术师 / 无面人共用的主动能力（参数化冷却）。
 * <p>
 * 消耗 10 米内最近的一个纸人，与其交换位置，同时清除自身一个负面效果。
 * 无面人版冷却大幅缩短，等效于「可用次数 +3」。
 */
public class PaperSubstituteAbility extends Ability {
    /** 纸人搜索半径（米） */
    private static final double RANGE = 10.0;

    public PaperSubstituteAbility(String path, int cooldownTicks) {
        // 消耗 6 灵性，主动能力
        super(GuimiMod.id(path), 6, cooldownTicks, true);
    }

    @Override
    public void onActivate(Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        // 深渊化克制：堕落之物的堕落状态下无法用替身脱身
        if (FallCorruptionEffect.isCorrupted(player)) {
            player.displayClientMessage(
                    Component.translatable("message.guimi_mod.fall_corruption.no_substitute"), true);
            return;
        }
        List<PaperFigurineEntity> figurines = level.getEntitiesOfClass(PaperFigurineEntity.class,
                player.getBoundingBox().inflate(RANGE));
        if (figurines.isEmpty()) {
            player.displayClientMessage(Component.translatable("ability.guimi_mod.paper_substitute.no_figurine"), true);
            return;
        }
        PaperFigurineEntity nearest = figurines.stream()
                .min(Comparator.comparingDouble(player::distanceToSqr))
                .orElseThrow();
        Vec3 dest = nearest.position();
        // 纸人被替身消耗
        nearest.discard();
        level.sendParticles(ParticleTypes.POOF,
                player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.3, 0.6, 0.3, 0.02);
        if (player instanceof ServerPlayer sp) {
            sp.teleportTo(level, dest.x, dest.y, dest.z, sp.getYRot(), sp.getXRot());
        } else {
            player.teleportTo(dest.x, dest.y, dest.z);
        }
        player.fallDistance = 0.0F;
        // 清除一个负面效果
        for (MobEffectInstance inst : List.copyOf(player.getActiveEffects())) {
            if (!inst.getEffect().value().isBeneficial()) {
                player.removeEffect(inst.getEffect());
                break;
            }
        }
        level.sendParticles(ParticleTypes.POOF, dest.x, dest.y + 1.0, dest.z, 20, 0.3, 0.6, 0.3, 0.02);
        level.playSound(null, player.blockPosition(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6F, 1.4F);
    }
}
