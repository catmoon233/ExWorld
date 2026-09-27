package com.wan.gmmod.content.abilities;

import com.wan.gmmod.GuimiMod;

/**
 * 「说服」——教唆者（魔女途径 · 序列 8）被动标记能力。
 * <p>
 * 与村民交互时交易价格约 -20%，且有小概率解锁一条隐藏交易。
 * 实际逻辑在 {@code WitchAbilityEventSubscriber#onVillagerInteract} 中处理：
 * 交互时施加短暂「村庄英雄」实现折扣，低概率向村民追加一条稀有交易。
 */
public class PersuadeAbility extends Ability {
    /** 解锁隐藏交易的概率 */
    public static final float HIDDEN_TRADE_CHANCE = 0.15F;

    public PersuadeAbility() {
        super(GuimiMod.id("persuade"));
    }
}
