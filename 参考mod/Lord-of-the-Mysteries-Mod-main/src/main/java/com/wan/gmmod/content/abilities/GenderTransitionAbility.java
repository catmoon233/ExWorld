package com.wan.gmmod.content.abilities;

import com.wan.gmmod.GuimiMod;

/**
 * 「性别转换」——女巫（魔女途径 · 序列 7）被动标记能力。
 * <p>
 * 男性玩家晋升女巫时自动切换为女性形态（写入 {@code ModAttachments#FEMALE_FORM}），
 * 永久生效，除非切换途径或降级。实际写入逻辑见 {@code PromotionHooks#onPromoted}，
 * 已安装 Female Gender Mod（wildfire_gender）时由 {@code FemaleGenderCompat}
 * 反射同步其性别设置，未安装时仅保留形态标记。
 */
public class GenderTransitionAbility extends Ability {
    public GenderTransitionAbility() {
        super(GuimiMod.id("gender_transition"));
    }
}
