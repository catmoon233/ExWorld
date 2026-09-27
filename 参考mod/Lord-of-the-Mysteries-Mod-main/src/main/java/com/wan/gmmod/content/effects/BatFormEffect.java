package com.wan.gmmod.content.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 「蝙蝠化形」隐藏标记效果：纯粹声明玩家正处于月亮 · 序列7 吸血鬼的蝙蝠化形
 * 持续生效期间，客户端据此隐藏玩家原本的玩家模型——本体化为黑雾与一群蝙蝠，
 * 不再以人类外形呈现。由蝙蝠化形能力激活时施加，随 buff 一同到期。
 */
public class BatFormEffect extends MobEffect {
    public BatFormEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x3a2a33);
    }
}