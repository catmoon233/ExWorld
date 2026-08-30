package net.exmo.exworld.client.battle;

/** Render-only movement curve. A path must keep the same speed when crossing cell boundaries. */
public final class BattleMovementInterpolator {
    private BattleMovementInterpolator() {}

    public static double segmentProgress(double progress) {
        return Math.max(0.0, Math.min(1.0, progress));
    }
}
