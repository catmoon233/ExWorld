package com.wan.gmmod.content.exp;

import com.wan.gmmod.Config;
import com.wan.gmmod.content.sequences.Sequences;

import java.util.EnumSet;
import java.util.Set;

/**
 * 实验性途径总开关。
 * <p>
 * 全部 22 条途径的能力均已正式实装并注册到 {@code AbilityRegistry}（愚者 / 魔女 / 战争之红，
 * 以及倒吊人 / 空想家 / 暴君 / 太阳 / 白塔 / 黄昏巨人 / 黑暗 / 死神，与 11 条原实验途径
 * 错误 / 门 / 完美者 / 隐者 / 月亮 / 母亲 / 深渊 / 被缚者 / 审判者 / 黑皇帝 / 命运之轮）。
 * 实验门控已移除：任何途径的能力默认全部生效。
 * <p>
 * 本类保留 {@code isEnabled / isLocked / isExperimental} 供历史接口调用，
 * 但由于实验集合为空，它们始终返回「未锁定」，不影响任何途径的能力。
 */
public final class ExperimentalPathways {

    /** 实验性途径集合（当前为空：全部途径均已正式实装）。 */
    private static final Set<Sequences.Pathway> EXPERIMENTAL = EnumSet.noneOf(Sequences.Pathway.class);

    /** 运行时覆盖值：null 表示未覆盖，跟随配置文件。 */
    private static Boolean runtimeOverride = null;

    private ExperimentalPathways() {}

    /** 实验能力当前是否启用。 */
    public static boolean isEnabled() {
        if (runtimeOverride != null) {
            return runtimeOverride;
        }
        try {
            return Config.EXPERIMENTAL_PATHWAYS_ENABLED.get();
        } catch (IllegalStateException e) {
            // 配置尚未加载（极早期调用），按默认关闭处理
            return false;
        }
    }

    /** 指令临时覆盖开关（null 恢复跟随配置）。 */
    public static void setRuntimeOverride(Boolean value) {
        runtimeOverride = value;
    }

    /** 该途径是否属于实验性途径。 */
    public static boolean isExperimental(Sequences.Pathway pathway) {
        return pathway != null && EXPERIMENTAL.contains(pathway);
    }

    /** 该途径的能力当前是否被锁定（实验途径且开关未打开）。 */
    public static boolean isLocked(Sequences.Pathway pathway) {
        return isExperimental(pathway) && !isEnabled();
    }
}
