package com.wan.gmmod.content.abilities;

import com.wan.gmmod.GuimiMod;

/**
 * 「镜子魔法强化」——欢愉魔女（魔女途径 · 序列 6）被动标记能力。
 * <p>
 * 镜子替身触发时有 50% 概率镜子不碎（锚点保留，近似"可选择附近镜子重生"，
 * 见 {@code MirrorSubstituteAbility#teleportToAnchor}）；
 * 镜子占卜成功率 / 精度提升（见 {@code MirrorDivinationPacket} 服务端结算）。
 */
public class MirrorMasteryAbility extends Ability {
    public MirrorMasteryAbility() {
        super(GuimiMod.id("mirror_mastery"));
    }
}
