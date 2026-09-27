package com.wan.gmmod.content.abilities;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.entities.BlackFlameEntity;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * 「操控黑焰」——女巫 / 欢愉魔女共用的主动能力（参数化朵数与伤害）。
 * <p>
 * 发射黑色火焰弹射物，仅对有灵性的目标有效（见 {@link BlackFlameEntity}），
 * 不点燃、不破坏地形。女巫基础版单朵 6 点；欢愉魔女升级版
 * （{@code black_flame_advanced}）7 朵霰射、每朵 8 点，且潜行触发时
 * 改为让自身燃烧黑焰驱除诅咒——清除所有负面效果。
 */
public class BlackFlameAbility extends Ability {
    private final int flameCount;
    private final float damage;

    public BlackFlameAbility(String path, int flameCount, float damage) {
        // 消耗 8 灵性，冷却 100 刻（5 秒），主动能力
        super(GuimiMod.id(path), 8, 100, true);
        this.flameCount = flameCount;
        this.damage = damage;
    }

    @Override
    public void onActivate(Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        // 升级版潜行触发：自燃净化——黑焰烧尽一切诅咒（清除全部负面效果）
        if (flameCount > 1 && player.isShiftKeyDown()) {
            List<Holder<MobEffect>> negatives = player.getActiveEffects().stream()
                    .filter(instance -> !instance.getEffect().value().isBeneficial())
                    .map(MobEffectInstance::getEffect)
                    .toList();
            negatives.forEach(player::removeEffect);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    40, 0.4, 0.8, 0.4, 0.05);
            level.playSound(null, player.blockPosition(),
                    SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.0F, 0.8F);
            player.displayClientMessage(Component.translatable("message.guimi_mod.black_flame.purge"), true);
            return;
        }
        // 发射黑焰：升级版 7 朵霰射，基础版 1 朵
        for (int i = 0; i < flameCount; i++) {
            BlackFlameEntity flame = new BlackFlameEntity(level, player, damage);
            flame.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
            float spread = flameCount > 1 ? 8.0F : 1.0F;
            flame.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.4F, spread);
            level.addFreshEntity(flame);
        }
        level.playSound(null, player.blockPosition(),
                SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.8F, 0.6F);
    }
}
