package com.wan.gmmod.content.abilities;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.entities.AirBulletEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/**
 * 「空气弹」——魔术师 / 无面人共用的主动能力（参数化伤害）。
 * <p>
 * 发射无形的压缩空气弹丸，冷却 2 秒。
 * 魔术师约 5 点伤害，无面人升级为 8 点（堪比步枪）。
 */
public class AirBulletAbility extends Ability {

    private final float damage;

    public AirBulletAbility(String path, float damage) {
        // 消耗 5 灵性，冷却 40 刻（2 秒），主动能力
        super(GuimiMod.id(path), 5, 40, true);
        this.damage = damage;
    }

    @Override
    public void onActivate(Player player) {
        if (player.level().isClientSide) {
            return;
        }
        AirBulletEntity bullet = new AirBulletEntity(player.level(), player, damage);
        bullet.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 3.0F, 0.0F);
        player.level().addFreshEntity(bullet);
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.6F);
    }
}
