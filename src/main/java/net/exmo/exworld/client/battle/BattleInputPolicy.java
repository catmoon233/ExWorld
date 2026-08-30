package net.exmo.exworld.client.battle;

/** Single seam for deciding whether battle world input owns a mouse event. */
public final class BattleInputPolicy {
    private BattleInputPolicy() {}

    public static boolean capturesMouse(boolean battleActive, boolean screenOpen) {
        return battleActive && !screenOpen;
    }
}
