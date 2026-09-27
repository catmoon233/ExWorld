package net.exmo.exworld.mystery.client;

import net.exmo.exworld.client.battle.BattleClient;
import net.exmo.exworld.client.camera.AdvancedCameraDirector;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Short, local camera motion. Battle camera always owns the view first. */
public final class MysteryCamera {
    private static Vec3 start;
    private static Vec3 target;
    private static Vec3 lookAt;
    private static int age;
    private static int duration;

    private MysteryCamera() {}

    public static void start(String cue, int ticks) { start(cue, ticks, 0); }

    public static void start(String cue, int ticks, int elapsed) {
        if (!cue.equals("identity_reveal") && !cue.equals("rewind") && !cue.equals("ending_chase")) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || AdvancedCameraDirector.active() || BattleClient.active()) return;
        start = mc.gameRenderer.getMainCamera().getPosition();
        lookAt = mc.player.getEyePosition().add(0, -.25, 0);
        Vec3 backward = mc.player.getLookAngle().scale(-2.5);
        target = lookAt.add(backward.x, cue.equals("rewind") ? 2.2 : 1.0, backward.z);
        age = Math.max(0, elapsed);
        duration = Math.max(1, Math.min(ticks, 80));
    }

    public static boolean active() {
        return start != null && age < duration && !AdvancedCameraDirector.active() && !BattleClient.active();
    }

    public static void tick() { if (start != null && ++age >= duration) clear(); }
    public static void clear() { start = target = lookAt = null; age = duration = 0; }

    private static float weight(float partial) {
        float p = Mth.clamp((age + partial) / duration, 0, 1);
        return Mth.sin((float)Math.PI * p) * .75F;
    }

    public static Vec3 position(float partial) { return start.lerp(target, weight(partial)); }
    public static float yaw(float partial) {
        Vec3 toward = lookAt.subtract(position(partial));
        return (float)(Math.atan2(toward.z, toward.x) * 180 / Math.PI) - 90;
    }
    public static float pitch(float partial) {
        Vec3 toward = lookAt.subtract(position(partial));
        return (float)(-Math.atan2(toward.y, Math.sqrt(toward.x * toward.x + toward.z * toward.z)) * 180 / Math.PI);
    }
    public static float fov(float partial) { return 70 - 8 * weight(partial); }
}
