package com.wan.gmmod.client;

/**
 * 模组 HUD 显隐的客户端状态（H 键切换）。
 * <p>
 * 仅影响本模组的 HUD 图层（左上角状态 HUD 与技能栏 HUD），
 * 不影响原版 GUI；纯客户端显示开关，无需与服务端同步。
 */
public final class HudClientState {
    private HudClientState() {}

    private static boolean hidden = false;

    public static boolean isHidden() {
        return hidden;
    }

    public static void toggle() {
        hidden = !hidden;
    }
}
