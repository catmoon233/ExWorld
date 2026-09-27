package com.wan.gmmod.content.divination;

/**
 * 黄水晶灵摆的三种摆动方向（纯视觉动画，与占卜结果的具体反馈解耦）。
 * <ul>
 *     <li>{@link #CLOCKWISE 顺时针}：偏向正面 / 有利的结果；</li>
 *     <li>{@link #COUNTERCLOCKWISE 逆时针}：偏向负面 / 反噬的结果；</li>
 *     <li>{@link #STILL 静止}：模糊、不明朗的结果。</li>
 * </ul>
 * 动画名由用户在 {@code pendulum.animation.json} 中提供。
 */
public enum PendulumSpin {
    CLOCKWISE("animation.pendulum.clockwise"),
    COUNTERCLOCKWISE("animation.pendulum.counterclockwise"),
    STILL("animation.pendulum.still");

    private final String animationName;

    PendulumSpin(String animationName) {
        this.animationName = animationName;
    }

    /** 对应的 GeckoLib 动画名 */
    public String getAnimationName() {
        return animationName;
    }

    /** 由序号安全地取回摆动方向（越界回退到 {@link #STILL}） */
    public static PendulumSpin byId(int id) {
        PendulumSpin[] values = values();
        return (id >= 0 && id < values.length) ? values[id] : STILL;
    }
}
