package com.wan.gmmod.content.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 「反占卜」隐藏 Buff：纯标记效果，无任何 tick 行为。
 * <p>
 * 由魔镜「反占卜 · 自我」模式施加（隐藏形式：ambient、无粒子、无图标，
 * 客户端扩展另行隐藏 HUD / 背包显示）。灵摆 / 魔镜占卜逻辑在选定目标玩家时
 * 校验其是否拥有该 Buff，命中则干扰占卜结果（见 {@code AntiDivination}）。
 */
public class AntiDivinationEffect extends MobEffect {
    public AntiDivinationEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xc0c8d0);
    }
}
