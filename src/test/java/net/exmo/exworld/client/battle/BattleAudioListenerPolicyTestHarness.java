package net.exmo.exworld.client.battle;

public final class BattleAudioListenerPolicyTestHarness {
    public static void main(String[] args) {
        String camera = "camera", player = "player";
        check(BattleAudioPolicy.choose(true, camera, player).equals(player), "battle listens from player presentation position");
        check(BattleAudioPolicy.choose(false, camera, player).equals(camera), "exploration restores camera listener position");
        check(BattleAudioPolicy.choose(true, camera, null).equals(camera), "missing player safely falls back to camera");
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
