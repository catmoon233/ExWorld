package net.exmo.exworld.client.perspective;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.phys.Vec3;

/**
 * Maintains an immutable sight-corridor snapshot for chunk rebuild threads. It never reads the client level from a
 * worker thread, which keeps the renderer hook safe and leaves all policy decisions in this module.
 */
final class DungeonOcclusionCuller {
    private static final double MIN_HEIGHT_ABOVE_FEET = 1.0;
    private static volatile Snapshot snapshot;
    private static BlockPos lastPlayerBlock;
    private static BlockPos lastCameraBlock;

    private DungeonOcclusionCuller() {}

    static void update(Minecraft minecraft, float yaw, float pitch, double cameraDistance) {
        if (minecraft.player == null || minecraft.level == null) {
            clear(minecraft);
            return;
        }

        Vec3 eye = minecraft.player.getEyePosition();
        Vec3 camera = ChunkGroupCameraBounds.cameraPosition(eye, yaw, pitch, cameraDistance);
        BlockPos playerBlock = minecraft.player.blockPosition();
        BlockPos cameraBlock = BlockPos.containing(camera);
        if (playerBlock.equals(lastPlayerBlock) && cameraBlock.equals(lastCameraBlock) && snapshot != null) return;

        Snapshot previous = snapshot;
        Snapshot next = new Snapshot(eye, camera, minecraft.player.getY() + MIN_HEIGHT_ABOVE_FEET);
        snapshot = next;
        lastPlayerBlock = playerBlock.immutable();
        lastCameraBlock = cameraBlock.immutable();
        dirty(minecraft, previous);
        dirty(minecraft, next);
    }

    static void clear(Minecraft minecraft) {
        Snapshot previous = snapshot;
        if (previous == null) return;
        snapshot = null;
        lastPlayerBlock = null;
        lastCameraBlock = null;
        dirty(minecraft, previous);
    }

    static boolean shouldCull(BlockState state, BlockPos pos) {
        Snapshot current = snapshot;
        if (current == null || state.hasBlockEntity() || !state.canOcclude()
                || state.getBlock() instanceof DoorBlock || state.getBlock() instanceof LadderBlock) return false;
        return current.contains(pos);
    }

    private static void dirty(Minecraft minecraft, Snapshot area) {
        if (area == null || minecraft.level == null) return;
        minecraft.levelRenderer.setBlocksDirty(area.minX, area.minY, area.minZ, area.maxX, area.maxY, area.maxZ);
    }

    private static final class Snapshot {
        private static final double PADDING = 3.0;
        private final Vec3 eye;
        private final Vec3 axis;
        private final double axisLengthSquared;
        private final double minimumY;
        private final int minX;
        private final int minY;
        private final int minZ;
        private final int maxX;
        private final int maxY;
        private final int maxZ;

        private Snapshot(Vec3 eye, Vec3 camera, double minimumY) {
            this.eye = eye;
            this.axis = camera.subtract(eye);
            this.axisLengthSquared = axis.lengthSqr();
            this.minimumY = minimumY;
            this.minX = (int) Math.floor(Math.min(eye.x, camera.x) - PADDING);
            this.minY = (int) Math.floor(Math.min(eye.y, camera.y) - 1.0);
            this.minZ = (int) Math.floor(Math.min(eye.z, camera.z) - PADDING);
            this.maxX = (int) Math.ceil(Math.max(eye.x, camera.x) + PADDING);
            this.maxY = (int) Math.ceil(Math.max(eye.y, camera.y) + PADDING);
            this.maxZ = (int) Math.ceil(Math.max(eye.z, camera.z) + PADDING);
        }

        private boolean contains(BlockPos pos) {
            if (axisLengthSquared < 0.01) return false;
            Vec3 center = Vec3.atCenterOf(pos);
            if (center.y < minimumY) return false;
            Vec3 fromEye = center.subtract(eye);
            double progress = fromEye.dot(axis) / axisLengthSquared;
            if (progress <= 0.06 || progress >= 1.04) return false;

            Vec3 closest = eye.add(axis.scale(progress));
            double radius = 0.8 + Math.min(1.0, progress) * 1.25;
            return center.distanceToSqr(closest) <= radius * radius;
        }
    }
}
