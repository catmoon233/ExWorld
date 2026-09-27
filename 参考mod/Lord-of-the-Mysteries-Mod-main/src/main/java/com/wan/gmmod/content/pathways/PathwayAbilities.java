package com.wan.gmmod.content.pathways;

/**
 * 全部途径能力总入口。
 * <p>
 * 22 条途径（愚者 / 魔女 / 战争之红 / 倒吊人 / 空想家 / 暴君 / 太阳 / 白塔 / 黄昏巨人 /
 * 黑暗 / 死神 / 完美者 / 隐者 / 命运之轮 / 审判者 / 黑皇帝 / 被缚者 / 深渊 / 月亮 / 母亲 /
 * 错误 / 门）序列 9~6 的全部能力统一注册到 {@code AbilityRegistry}。
 * <p>
 * 原实验途径已并入正式体系，直接生效，不受
 * {@code com.wan.gmmod.content.exp.ExperimentalPathways} 门控。
 */
public final class PathwayAbilities {

    private PathwayAbilities() {}

    public static void init() {
        PathwayAbilitiesP1.init(); // 倒吊人 / 空想家 / 暴君 / 太阳
        PathwayAbilitiesP2.init(); // 白塔 / 黄昏巨人 / 黑暗 / 死神
        // 原实验途径（完美者 / 隐者 / 命运之轮 / 审判者 / 黑皇帝 / 被缚者 / 深渊 / 月亮 / 母亲 / 错误 / 门）
        // 已注册为正式途径，统一在此入口生效
        com.wan.gmmod.content.exp.ExpAbilities.init();
    }
}