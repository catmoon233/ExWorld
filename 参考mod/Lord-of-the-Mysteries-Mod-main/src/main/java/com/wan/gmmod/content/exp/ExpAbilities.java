package com.wan.gmmod.content.exp;

/**
 * 原实验途径能力注册入口（现为正式途径，直接生效）。
 * <p>
 * 11 条原实验途径（完美者 / 隐者 / 命运之轮 / 审判者 / 黑皇帝 / 被缚者 / 深渊 / 月亮 / 母亲 /
 * 错误 / 门）序列 9~6 的能力注册到 {@code AbilityRegistry}。
 * <p>
 * 实验门控已移除（见 {@link ExperimentalPathways}：实验集合为空，恒不锁定），
 * 由 {@code content.pathways.PathwayAbilities} 统一入口调用，直接生效。
 */
public final class ExpAbilities {

    private ExpAbilities() {}

    public static void init() {
        ExpAbilitiesP3.init(); // 完美者 / 隐者 / 命运之轮 / 审判者
        ExpAbilitiesP4.init(); // 黑皇帝 / 被缚者 / 深渊
        ExpAbilitiesP5.init(); // 月亮 / 母亲 / 错误 / 门
    }
}
