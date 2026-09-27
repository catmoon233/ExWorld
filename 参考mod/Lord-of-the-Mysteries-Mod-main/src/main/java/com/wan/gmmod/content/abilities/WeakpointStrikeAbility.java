package com.wan.gmmod.content.abilities;

import com.wan.gmmod.GuimiMod;

/**
 * 「弱点打击」——刺客（魔女途径 · 序列 9）被动标记能力。
 * <p>
 * 潜行状态下的首次攻击伤害 ×2（背刺），冷却 5 秒。
 * 实际倍伤逻辑在 {@code WitchAbilityEventSubscriber#onBackstab} 中处理，
 * 冷却记录于 {@code ModAttachments#BACKSTAB_COOLDOWN_END}。
 */
public class WeakpointStrikeAbility extends Ability {
    /** 背刺冷却（刻，5 秒） */
    public static final int BACKSTAB_COOLDOWN = 100;
    /** 背刺伤害倍率 */
    public static final float DAMAGE_MULTIPLIER = 2.0F;

    public WeakpointStrikeAbility() {
        super(GuimiMod.id("weakpoint_strike"));
    }
}
