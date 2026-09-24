package net.exmo.exworld.client.perspective;

import net.exmo.exworld.Exworld;
import net.exmo.exworld.client.battle.BattleClient;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * V-key toggle between the exploration god-view and first-person.
 *
 * <p>Outside combat the world is normally shown through {@link DungeonPerspective}'s detached god-view camera.
 * Pressing the toggle starts a short easing transition that swoops the camera between the god-view pose and the
 * player's eyes, then either installs the vanilla first-person profile or returns to the remembered god-view pose.
 * The player-facing state change is announced with an action-bar message.</p>
 */
public final class FirstPersonToggle {
    public static final ResourceLocation OWNER = ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "first_person");
    private static final int DURATION = 14;

    private static boolean firstPerson;
    private static boolean transitioning;
    private static int tick;

    private static float startYaw, startPitch, startDistance;
    private static float endYaw, endPitch, endDistance;

    /** God-view pose captured when leaving it, so the return transition lands exactly where the player left off. */
    private static float savedGodYaw = CameraProfile.EXPLORATION.yaw();
    private static float savedGodPitch = CameraProfile.EXPLORATION.pitch();
    private static float savedGodDistance = CameraProfile.EXPLORATION.distance();

    private FirstPersonToggle() {}

    public static boolean firstPerson() { return firstPerson; }
    public static boolean transitioning() { return transitioning; }

    public static void toggle() {
        Minecraft minecraft = Minecraft.getInstance();
        if (net.exmo.exworld.Config.decryptionMode) {
            if (minecraft.player == null || !minecraft.player.isCreative()) {
                if (minecraft.player != null) {
                    minecraft.player.displayClientMessage(Component.translatable("message.exworld.perspective_locked"), true);
                }
                return;
            }
            creativeGodView = firstPerson;
        }
        toggleInternal();
    }

    /** Decryption mode keeps non-creative players, and creative players who have not opted in, out of the god view. */
    public static boolean blocksDungeonView() {
        return net.exmo.exworld.Config.decryptionMode && !creativeGodViewAllowed();
    }

    public static void enforceDecryption() {
        if (!net.exmo.exworld.Config.decryptionMode) {
            creativeGodView = false;
            return;
        }
        if (creativeGodViewAllowed()) return;
        creativeGodView = false;
        firstPerson = true;
        transitioning = false;
        DungeonPerspective.setOverride(OWNER, CameraProfile.VANILLA);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) minecraft.options.setCameraType(CameraType.FIRST_PERSON);
    }

    private static boolean creativeGodViewAllowed() {
        Minecraft minecraft = Minecraft.getInstance();
        return creativeGodView && minecraft.player != null && minecraft.player.isCreative();
    }

    private static boolean creativeGodView;

    private static void toggleInternal() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null) return;
        if (minecraft.player.isSpectator() || minecraft.player.isDeadOrDying()) return;
        if (BattleClient.active()) return; // only outside combat

        firstPerson = !firstPerson;
        tick = 0;
        transitioning = true;

        if (firstPerson) {
            savedGodYaw = DungeonPerspective.cameraYaw();
            savedGodPitch = DungeonPerspective.cameraPitch();
            savedGodDistance = DungeonPerspective.zoomDistance();

            startYaw = savedGodYaw;
            startPitch = savedGodPitch;
            startDistance = savedGodDistance;
            endYaw = minecraft.player.getYRot();
            endPitch = 0.0F;
            endDistance = 0.0F;
        } else {
            startYaw = minecraft.player.getYRot();
            startPitch = minecraft.player.getXRot();
            startDistance = 0.0F;
            endYaw = savedGodYaw;
            endPitch = savedGodPitch;
            endDistance = savedGodDistance;

            // Re-engage the detached god-view camera so the rise has a camera path to drive.
            DungeonPerspective.clearOverride(OWNER);
            minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }

        DungeonPerspective.applyTransition(startYaw, startPitch, startDistance);
        minecraft.player.displayClientMessage(Component.translatable(
                firstPerson ? "message.exworld.first_person_on" : "message.exworld.first_person_off"), true);
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (transitioning && (minecraft.player == null || BattleClient.active())) {
            // A combat or world teardown interrupted the animation; abort without changing the mode.
            transitioning = false;
            tick = 0;
            if (firstPerson) {
                firstPerson = false;
                DungeonPerspective.clearOverride(OWNER);
            }
            DungeonPerspective.endTransition(savedGodDistance);
            if (minecraft.player != null) minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            return;
        }

        if (firstPerson && !transitioning) {
            // Keep the first-person profile pinned, including when a battle that overrode it ends.
            if (!BattleClient.active()) minecraft.options.setCameraType(CameraType.FIRST_PERSON);
            return;
        }

        if (!transitioning) return;

        tick++;
        float t = Mth.clamp(tick / (float) DURATION, 0.0F, 1.0F);
        float eased = t * t * (3.0F - 2.0F * t);
        DungeonPerspective.applyTransition(
                Mth.rotLerp(eased, startYaw, endYaw),
                Mth.lerp(eased, startPitch, endPitch),
                Mth.lerp(eased, startDistance, endDistance));
        if (t >= 1.0F) complete();
    }

    private static void complete() {
        transitioning = false;
        tick = 0;
        Minecraft minecraft = Minecraft.getInstance();
        if (firstPerson && minecraft.player != null) {
            // Lock the view direction before handing control to the first-person camera.
            minecraft.player.setYRot(DungeonPerspective.cameraYaw());
            minecraft.player.setXRot(DungeonPerspective.cameraPitch());
            minecraft.player.setYHeadRot(DungeonPerspective.cameraYaw());
            DungeonPerspective.setOverride(OWNER, CameraProfile.VANILLA);
            minecraft.options.setCameraType(CameraType.FIRST_PERSON);
        }
        DungeonPerspective.endTransition(endDistance);
    }
}
