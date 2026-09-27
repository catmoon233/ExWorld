package com.wan.gmmod.content.abilities;

import com.wan.gmmod.GuimiMod;

/**
 * 「魔镜占卜」——女巫（魔女途径 · 序列 7）被动标记能力。
 * <p>
 * 手持镜子物品右键打开占卜 GUI（见 {@code MirrorItem} 与
 * {@code MirrorDivinationScreen}），可选择「占卜」「反占卜」「通灵」三种模式，
 * 结果逻辑复用灵摆占卜但更精准。灵性消耗与冷却在
 * {@code MirrorDivinationPacket} 服务端处理时结算。
 */
public class MirrorDivinationAbility extends Ability {
    /** 每次占卜消耗的灵性 */
    public static final int COST = 10;
    /** 占卜冷却（刻，20 秒） */
    public static final int COOLDOWN = 20 * 20;

    public MirrorDivinationAbility() {
        super(GuimiMod.id("mirror_divination"));
    }
}
