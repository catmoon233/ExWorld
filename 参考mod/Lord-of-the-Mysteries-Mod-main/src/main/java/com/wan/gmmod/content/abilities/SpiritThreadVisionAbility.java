package com.wan.gmmod.content.abilities;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.capability.ModAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * 秘偶大师「灵体之线视野」——序列 5 切换能力。
 * <p>
 * 开启 / 关闭灵体之线视角（复用灵视附件与 V 键框架）：
 * 看到周围生物身上发光的灵体之线；隐身 / 阴影中的目标同样显形（反隐）。
 */
public class SpiritThreadVisionAbility extends Ability {
    /** 灵体之线可视半径（米） */
    private static final double RADIUS = 30.0;

    public SpiritThreadVisionAbility() {
        // 消耗 2 灵性，冷却 20 刻（1 秒防连点），主动切换能力
        super(GuimiMod.id("spirit_thread_vision"), 2, 20, true);
    }

    @Override
    public void onActivate(Player player) {
        if (player.level().isClientSide) {
            return;
        }
        if (!com.wan.gmmod.content.vision.SpiritVision.allowed(player)) {
            return;
        }
        boolean enabled = !Boolean.TRUE.equals(player.getData(ModAttachments.SPIRIT_VISION));
        player.setData(ModAttachments.SPIRIT_VISION, enabled);
        if (enabled) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, false, false, false));
        } else {
            player.removeEffect(MobEffects.NIGHT_VISION);
        }
    }

    @Override
    public void onPassiveTick(Player player) {
        if (player.level().isClientSide || player.tickCount % 20 != 0) {
            return;
        }
        if (!player.getData(ModAttachments.SPIRIT_VISION)) {
            return;
        }
        // 灵体之线：周围生物显现发光的线，隐身目标同样显形（发光无视隐身 → 反隐）
        for (LivingEntity living : player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(RADIUS), e -> e != player && e.isAlive())) {
            living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 45, 0, false, false, false));
        }
    }
}
