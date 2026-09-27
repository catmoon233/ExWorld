package com.wan.gmmod.content.war;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.abilities.Ability;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/**
 * 「智力提升」——阴谋家（战争之红途径 · 序列 6）被动。
 * <p>
 * 常驻「村庄英雄」效果实现村民交易折扣（约 -15%）；
 * 说服成功率翻倍在 WarAbilityEventSubscriber 中与教唆者的说服叠加处理。
 */
public class IntellectBoostAbility extends Ability {

    public IntellectBoostAbility() {
        super(GuimiMod.id("intellect_boost"));
    }

    @Override
    public void onPassiveTick(Player player) {
        if (player.level().isClientSide) {
            return;
        }
        // 常驻刷新（220 刻余量避免闪烁），ambient 且不显示粒子
        MobEffectInstance current = player.getEffect(MobEffects.HERO_OF_THE_VILLAGE);
        if (current == null || current.getDuration() < 200) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.HERO_OF_THE_VILLAGE, 220, 0, true, false));
        }
    }
}
