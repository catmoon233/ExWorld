package com.wan.gmmod.client;

import net.minecraft.Util;

/**
 * 技能页切换的客户端状态（3 页 × 5 槽，共 15 个全局槽位）。
 * <p>
 * 交互方案：Alt+滚轮切换技能页；Ctrl+滚轮页内选择技能槽（左/右键释放选中技能）；
 * 中键短按顺序切页、长按打开技能配置界面；侧键 M4/M5 前后翻页、双侧键同按直达紧急技能页。
 * 纯客户端状态：页码只影响"前 5 个技能键 / 选中释放"映射到哪 5 个全局槽位，无需与服务端同步。
 */
public final class SkillPageClientState {
    private SkillPageClientState() {}

    /** 技能页数 */
    public static final int PAGES = 3;
    /** 每页槽位数 */
    public static final int SLOTS_PER_PAGE = 5;
    /** 紧急技能页（保命页，HUD 边框显示为红色）索引 */
    public static final int EMERGENCY_PAGE = 2;

    /** 中键长按判定阈值（毫秒），超过则打开技能配置界面 */
    private static final long LONG_PRESS_MS = 300L;
    /** 页码指示显示总时长（刻），末段渐隐 */
    private static final int INDICATOR_TICKS = 40;
    /** 页码指示渐隐段时长（刻） */
    private static final int INDICATOR_FADE_TICKS = 15;
    /** 切页后当前页边框闪烁时长（刻） */
    private static final int FLASH_TICKS = 12;

    private static int page = 0;
    /** 页内选中槽（0~4），-1 表示未选中 */
    private static int selectedSlot = -1;
    private static int indicatorTicks = 0;
    private static int flashTicks = 0;
    /** 中键按下时间戳（毫秒），-1 表示未按下或已被长按消费 */
    private static long middleDownMillis = -1L;
    private static boolean forwardDown = false;
    private static boolean backDown = false;

    public static int getPage() {
        return page;
    }

    public static boolean isEmergencyPage() {
        return page == EMERGENCY_PAGE;
    }

    public static int getSelectedSlot() {
        return selectedSlot;
    }

    /** 页内选中槽对应的全局槽位索引（0~14），未选中返回 -1。 */
    public static int getSelectedGlobalSlot() {
        return selectedSlot < 0 ? -1 : page * SLOTS_PER_PAGE + selectedSlot;
    }

    /** 当前页的局部槽位（0~4）→ 全局槽位索引（0~14）。 */
    public static int globalSlot(int localSlot) {
        return page * SLOTS_PER_PAGE + localSlot;
    }

    /** 顺序切页：delta = +1 下一页 / -1 上一页（循环）。 */
    public static void switchPage(int delta) {
        page = Math.floorMod(page + delta, PAGES);
        onPageChanged();
    }

    /** 双侧键同按：直达紧急技能页。 */
    public static void gotoEmergencyPage() {
        page = EMERGENCY_PAGE;
        onPageChanged();
    }

    private static void onPageChanged() {
        selectedSlot = -1;
        indicatorTicks = INDICATOR_TICKS;
        flashTicks = FLASH_TICKS;
    }

    /** Ctrl+滚轮：在当前页 5 个槽之间循环选择。 */
    public static void scrollSelect(int dir) {
        if (selectedSlot < 0) {
            selectedSlot = dir > 0 ? 0 : SLOTS_PER_PAGE - 1;
        } else {
            selectedSlot = Math.floorMod(selectedSlot + dir, SLOTS_PER_PAGE);
        }
        indicatorTicks = INDICATOR_TICKS;
    }

    /** 释放技能或切页后清除页内选中。 */
    public static void clearSelection() {
        selectedSlot = -1;
    }

    // ===== 中键短按 / 长按 =====

    public static void onMiddlePress() {
        middleDownMillis = Util.getMillis();
    }

    /** 中键松开：短按返回 true（调用方执行切页），长按已在 {@link #tick()} 中消费。 */
    public static boolean onMiddleRelease() {
        boolean shortPress = middleDownMillis > 0
                && Util.getMillis() - middleDownMillis < LONG_PRESS_MS;
        middleDownMillis = -1L;
        return shortPress;
    }

    // ===== 侧键按住状态（用于 M4+M5 组合判定） =====

    public static void setForwardDown(boolean down) {
        forwardDown = down;
    }

    public static void setBackDown(boolean down) {
        backDown = down;
    }

    public static boolean isForwardDown() {
        return forwardDown;
    }

    public static boolean isBackDown() {
        return backDown;
    }

    // ===== HUD 视觉反馈 =====

    /** 切页闪烁：闪烁期内以 2 刻为周期明暗交替。 */
    public static boolean isFlashBright() {
        return flashTicks > 0 && (flashTicks / 2) % 2 == 0;
    }

    public static boolean isFlashing() {
        return flashTicks > 0;
    }

    /** 页码指示透明度（0~1）：先保持完全可见，末段渐隐。 */
    public static float getIndicatorAlpha() {
        if (indicatorTicks <= 0) {
            return 0.0F;
        }
        return Math.min(1.0F, indicatorTicks / (float) INDICATOR_FADE_TICKS);
    }

    /** 每客户端刻调用：递减计时并处理中键长按（打开技能配置界面）。 */
    public static void tick() {
        if (indicatorTicks > 0) {
            indicatorTicks--;
        }
        if (flashTicks > 0) {
            flashTicks--;
        }
        // 技能配置界面已停用，中键长按不再打开。
        if (middleDownMillis > 0 && Util.getMillis() - middleDownMillis >= LONG_PRESS_MS) {
            middleDownMillis = -1L;
        }
    }
}
