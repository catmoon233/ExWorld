package com.wan.gmmod.content.abilities;

import com.wan.gmmod.GuimiMod;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/**
 * 「鹰眼视力」——刺客（魔女途径 · 序列 9）被动。
 * <p>
 * 常驻夜视；每 2 秒为 32 格内的远处（12 格以外）生物附加短暂发光，
 * 形成类似望远镜的轮廓高亮效果。
 */
public class EagleEyeAbility extends Ability {
    /** 高亮最大距离（格） */
    private static final double HIGHLIGHT_RANGE = 32.0;
    /** 近处不高亮的距离（格），避免近战时满屏描边 */
    private static final double NEAR_RANGE = 12.0;
    /** 高亮刷新间隔（刻） */
    private static final int REFRESH_INTERVAL = 40;

    public EagleEyeAbility() {
        super(GuimiMod.id("eagle_eye"));
    }

    @Override
    public void onPassiveTick(Player player) {
        if (player.level().isClientSide) {
            return;
        }
        // 常驻夜视（每 tick 刷新，保持不闪烁）
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 220, 0, true, false));

        // 远处生物轮廓高亮
        if (player.tickCount % REFRESH_INTERVAL != 0) {
            return;
        }
        AABB box = player.getBoundingBox().inflate(HIGHLIGHT_RANGE);
        for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive())) {
            double dist = entity.distanceTo(player);
            if (dist >= NEAR_RANGE && dist <= HIGHLIGHT_RANGE) {
                entity.addEffect(new MobEffectInstance(MobEffects.GLOWING,
                        REFRESH_INTERVAL + 20, 0, false, false));
            }
        }
    }
}
