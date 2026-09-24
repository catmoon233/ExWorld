package net.exmo.exworld.client.perspective;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import org.joml.Vector3f;
import net.neoforged.neoforge.client.event.CalculateDetachedCameraDistanceEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.event.InputEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import net.exmo.exworld.client.battle.BattleClient;
import net.exmo.exworld.mixin.client.GameRendererAccessor;

/**
 * Deep camera module: ordinary exploration gets a dungeon view, while later systems can replace the entire policy
 * through one named override and remove it when their special situation ends.
 */
public final class DungeonPerspective {
    private static final Map<ResourceLocation, CameraProfile> OVERRIDES = new LinkedHashMap<>();
    private static float cameraYaw = CameraProfile.EXPLORATION.yaw();
    private static float cameraPitch = CameraProfile.EXPLORATION.pitch();
    private static boolean wasActive;
    private static CameraProfile lastProfile;
    private static double lastDragX;
    private static double lastDragY;
    private static boolean middleDragging;
    private static final CameraZoomController ZOOM = new CameraZoomController(CameraProfile.EXPLORATION.distance());
    /** Distance driven by {@link FirstPersonToggle} while a transition animates; -1 means use the zoom controller. */
    private static float transitionDistance = -1.0F;

    private DungeonPerspective() {}

    public static CameraProfile profile() {
        CameraProfile active = CameraProfile.EXPLORATION;
        for (CameraProfile override : OVERRIDES.values()) active = override;
        return active;
    }

    public static void setOverride(ResourceLocation owner, CameraProfile profile) {
        OVERRIDES.remove(owner);
        OVERRIDES.put(owner, profile);
    }

    public static void clearOverride(ResourceLocation owner) { OVERRIDES.remove(owner); }

    /** Sets the camera pose directly for a smooth transition; distance may be zero to reach the player's eyes. */
    static void applyTransition(float yaw, float pitch, float distance) {
        cameraYaw = yaw;
        cameraPitch = pitch;
        transitionDistance = Math.max(0.0F, distance);
    }

    /** Ends a transition and resumes normal zoom at the given distance. */
    static void endTransition(float resumeDistance) {
        transitionDistance = -1.0F;
        ZOOM.reset(resumeDistance);
    }

    private static float renderDistance(float partialTick) {
        return transitionDistance >= 0.0F ? transitionDistance : ZOOM.sample(partialTick);
    }

    public static boolean active() {
        Minecraft minecraft = Minecraft.getInstance();
        return !FirstPersonToggle.blocksDungeonView() && profile().dungeonView() && minecraft.player != null && minecraft.level != null
                && !minecraft.player.isSpectator() && !minecraft.player.isDeadOrDying();
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!active()) {
            if (wasActive && minecraft.screen == null) {
                // Restore vanilla's captured cursor when a combat/cutscene profile takes control.
                minecraft.mouseHandler.releaseMouse();
                minecraft.mouseHandler.grabMouse();
            }
            wasActive = false;
            middleDragging = false;
            DungeonOcclusionCuller.clear(minecraft);
            return;
        }
        CameraProfile profile = profile();
        minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        if (!wasActive || !profile.equals(lastProfile)) {
            cameraYaw = profile.yaw();
            cameraPitch = profile.pitch();
            ZOOM.reset(profile.distance());
            minecraft.player.setYRot(cameraYaw);
            minecraft.player.setXRot(cameraPitch);
            minecraft.player.setYHeadRot(cameraYaw);
            minecraft.player.yHeadRotO = cameraYaw;
            minecraft.player.yBodyRot = cameraYaw;
            minecraft.player.yBodyRotO = cameraYaw;
            // MouseHandler still considers the mouse grabbed so clicks reach the game, while our
            // grabMouse redirect keeps the operating-system cursor visible and unconstrained.
            if (minecraft.screen == null) {
                minecraft.mouseHandler.releaseMouse();
                minecraft.mouseHandler.grabMouse();
            }
            wasActive = true;
            lastProfile = profile;
        }
        updateMiddleMouseCamera(minecraft);
        if (!BattleClient.active() && !FirstPersonToggle.transitioning()) {
            if (!middleDragging) aimPlayerAtCursor(minecraft.player);
        }
        ZOOM.tick();
        // The player owns live look input. Never rewrite it per tick: doing so fights MouseHandler and causes jitter.
        if (BattleClient.active()) DungeonOcclusionCuller.clear(minecraft);
        else {
            double distance = ChunkGroupCameraBounds.constrainedDistance(
                    minecraft.player.getEyePosition(), cameraYaw, cameraPitch, renderDistance(1.0F));
            DungeonOcclusionCuller.update(minecraft, cameraYaw, cameraPitch, distance);
        }
    }

    public static void computeAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!active()) return;
        if (net.exmo.exworld.client.camera.AdvancedCameraDirector.active()) {
            event.setYaw(net.exmo.exworld.client.camera.AdvancedCameraDirector.yaw((float) event.getPartialTick()));
            event.setPitch(net.exmo.exworld.client.camera.AdvancedCameraDirector.pitch((float) event.getPartialTick())); event.setRoll(0); return;
        }
        event.setYaw(cameraYaw);
        event.setPitch(cameraPitch);
        event.setRoll(0.0F);
    }

    public static void cameraDistance(CalculateDetachedCameraDistanceEvent event) {
        if (!active()) return;
        Minecraft minecraft = Minecraft.getInstance();
        float partialTick = event.getCamera().getPartialTickTime();
        double physicalDistance = BattleClient.active() ? renderDistance(partialTick)
                : ChunkGroupCameraBounds.constrainedDistance(minecraft.player.getEyePosition(partialTick), cameraYaw, cameraPitch,
                    renderDistance(partialTick));
        float scale = event.getEntityScalingFactor();
        event.setDistance(scale > 0.0F ? (float) (physicalDistance / scale) : 0.0F);
    }

    public static boolean shouldCull(BlockState state, BlockPos pos) {
        return DungeonOcclusionCuller.shouldCull(state, pos);
    }

    /** Absolute cursor targeting: the dungeon camera stays stable while the player turns towards the world cursor. */
    public static void aimPlayerAtCursor(LocalPlayer player) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!active() || minecraft.screen != null) return;
        double width = minecraft.getWindow().getScreenWidth();
        double height = minecraft.getWindow().getScreenHeight();
        if (width <= 0.0 || height <= 0.0) return;

        double ndcX = minecraft.mouseHandler.xpos() / width * 2.0 - 1.0;
        double ndcY = 1.0 - minecraft.mouseHandler.ypos() / height * 2.0;
        var camera = minecraft.gameRenderer.getMainCamera();
        float fov = CursorProjection.fovForCursorRay(minecraft.options.fov().get(),
                (float) ((GameRendererAccessor) minecraft.gameRenderer).exworld$renderedFov(camera, camera.getPartialTickTime(), true));
        Vector3f direction = CursorProjection.direction(new Vector3f(camera.getLookVector()), new Vector3f(camera.getLeftVector()),
                new Vector3f(camera.getUpVector()), ndcX, ndcY, width / height, fov);
        Vec3 start = camera.getPosition();
        Vec3 end = start.add(direction.x * 128.0, direction.y * 128.0, direction.z * 128.0);
        HitResult hit = player.level().clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        Vec3 target = hit.getType() == HitResult.Type.MISS ? groundIntersection(start, direction, player.getY()) : hit.getLocation();
        player.lookAt(EntityAnchorArgument.Anchor.EYES, target);
    }

    private static Vec3 groundIntersection(Vec3 start, Vector3f direction, double groundY) {
        if (direction.y < -1.0E-4F) {
            double distance = (groundY - start.y) / direction.y;
            if (distance > 0.0) return start.add(direction.x * distance, direction.y * distance, direction.z * distance);
        }
        return start.add(direction.x * 32.0, direction.y * 32.0, direction.z * 32.0);
    }

    /** Middle-button drag rotates the detached dungeon view without rotating the player or capturing the cursor. */
    public static void updateMiddleMouseCamera(Minecraft minecraft) {
        if (FirstPersonToggle.transitioning()) { middleDragging = false; return; }
        boolean pressed = minecraft.screen == null && minecraft.mouseHandler.isMiddlePressed();
        double x = minecraft.mouseHandler.xpos();
        double y = minecraft.mouseHandler.ypos();
        if (pressed && middleDragging) {
            cameraYaw = Mth.wrapDegrees(cameraYaw + (float) ((x - lastDragX) * 0.28));
            cameraPitch = Mth.clamp(cameraPitch + (float) ((y - lastDragY) * 0.22), 28.0F, 72.0F);
        }
        middleDragging = pressed;
        lastDragX = x;
        lastDragY = y;
    }

    /** Ctrl + scroll wheel zoom for gameplay; Screens receive scrolling themselves and never reach this handler. */
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!active() || minecraft.screen != null || (!BattleClient.active() && !Screen.hasControlDown()) || event.getScrollDeltaY() == 0.0) return;
        ZOOM.scroll(event.getScrollDeltaY());
        event.setCanceled(true);
    }

    public static float cameraYaw() { return cameraYaw; }
    public static float cameraPitch() { return cameraPitch; }
    public static float zoomDistance() { return ZOOM.current(); }
}
