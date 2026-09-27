package com.wan.gmmod.content.abilities;

import com.wan.gmmod.GuimiMod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/**
 * 「阴影躲藏」——刺客（魔女途径 · 序列 9）被动。
 * <p>
 * 所处位置亮度 ≤ 4 时获得隐身效果。并非完全隐形——原版隐身状态下
 * 生物的探测范围会大幅缩小（约等效 -50% 以上），符合「生物探测范围减半」的设定。
 */
public class ShadowHidingAbility extends Ability {
    /** 触发隐身的亮度阈值 */
    private static final int LIGHT_THRESHOLD = 4;

    public ShadowHidingAbility() {
        super(GuimiMod.id("shadow_hiding"));
    }

    @Override
    public void onPassiveTick(Player player) {
        if (player.level().isClientSide || player.tickCount % 10 != 0) {
            return;
        }
        BlockPos pos = player.blockPosition();
        int light = player.level().getMaxLocalRawBrightness(pos);
        if (light <= LIGHT_THRESHOLD) {
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 30, 0, true, false));
        }
    }
}
