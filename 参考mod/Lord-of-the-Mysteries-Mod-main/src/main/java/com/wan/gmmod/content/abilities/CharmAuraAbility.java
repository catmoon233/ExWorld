package com.wan.gmmod.content.abilities;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.charm.CharmManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/**
 * 「魅力提升」——欢愉魔女（魔女途径 · 序列 6）被动。
 * <p>
 * 10 米内生物有概率被魅惑——教唆者魅惑的弱化版：仅中立化（清空攻击目标），
 * 不会跟随。每 3 秒对范围内每个未魅惑生物独立判定一次。
 */
public class CharmAuraAbility extends Ability {
    /** 光环半径（格） */
    private static final double RANGE = 10.0;
    /** 判定间隔（刻，3 秒） */
    private static final int CHECK_INTERVAL = 60;
    /** 单次判定魅惑概率 */
    private static final float CHARM_CHANCE = 0.15F;
    /** 弱化魅惑持续时间（刻，10 秒） */
    private static final int DURATION = 10 * 20;

    public CharmAuraAbility() {
        super(GuimiMod.id("charm_aura"));
    }

    @Override
    public void onPassiveTick(Player player) {
        if (!(player instanceof ServerPlayer sp) || sp.tickCount % CHECK_INTERVAL != 0) {
            return;
        }
        AABB box = sp.getBoundingBox().inflate(RANGE);
        for (Mob mob : sp.level().getEntitiesOfClass(Mob.class, box,
                m -> m.isAlive() && !CharmManager.isCharmed(m))) {
            if (sp.getRandom().nextFloat() < CHARM_CHANCE) {
                CharmManager.calm(mob, sp, DURATION);
            }
        }
    }
}
