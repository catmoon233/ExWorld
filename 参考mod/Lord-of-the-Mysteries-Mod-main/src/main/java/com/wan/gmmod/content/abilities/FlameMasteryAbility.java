package com.wan.gmmod.content.abilities;

import com.wan.gmmod.GuimiMod;

/**
 * 秘偶大师「火焰操纵强化」——序列 5 被动能力。
 * <p>
 * 可凭空召唤焰流：解锁后「操纵火焰」点燃地面时扩大为 3×3 焰流
 * （实际增强逻辑见 {@link FlameControlAbility}），范围仍为 30 米。
 */
public class FlameMasteryAbility extends Ability {

    public FlameMasteryAbility() {
        super(GuimiMod.id("flame_mastery"));
    }
}
