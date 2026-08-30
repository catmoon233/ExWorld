package net.exmo.exworld.client.battle;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/** Pure policy seam used by the sound mixin and regression harness. */
public final class BattleAudioPolicy {
    private BattleAudioPolicy() {}
    public static <T> T choose(boolean battleActive, T cameraPosition, T playerPosition) {
        return battleActive && playerPosition != null ? playerPosition : cameraPosition;
    }
    public static Vec3 listenerPosition(boolean battleActive, Vec3 cameraPosition, Vec3 playerEyePosition) {
        return choose(battleActive, cameraPosition, playerEyePosition);
    }
    public static Vec3 listenerPosition(Vec3 pos) {
        Minecraft minecraft = Minecraft.getInstance();
        Vec3 player = minecraft.player == null ? null : minecraft.player.getEyePosition();
        if (minecraft.player != null) {
            Vec3 presented = BattleClient.presentationPosition(minecraft.player.getUUID(), 1.0F);
            if (presented != null) player = presented.add(0, minecraft.player.getEyeHeight(), 0);
        }
        return listenerPosition(BattleClient.active(), pos, player);
    }
}
