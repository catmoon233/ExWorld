package com.wan.gmmod.content.abilities;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.entities.IceSpearEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/**
 * 「冰晶长枪」——欢愉魔女（魔女途径 · 序列 6）主动（冰霜强化的一部分）。
 * <p>
 * 投掷冰晶长枪：伤害 8 点，命中后目标冰冻 3 秒 + 减速 5 秒（见
 * {@link IceSpearEntity}）。消耗 10 灵性，冷却 10 秒。
 */
public class IceSpearAbility extends Ability {
    public IceSpearAbility() {
        super(GuimiMod.id("ice_spear"), 10, 200, true);
    }

    @Override
    public void onActivate(Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        IceSpearEntity spear = new IceSpearEntity(level, player);
        spear.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
        spear.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.8F, 0.5F);
        level.addFreshEntity(spear);
        level.playSound(null, player.blockPosition(),
                SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 1.0F, 1.4F);
    }
}
