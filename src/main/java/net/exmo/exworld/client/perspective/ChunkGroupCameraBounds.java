package net.exmo.exworld.client.perspective;

import net.exmo.exworld.client.ClientChunkGroupState;
import net.exmo.exworld.client.ChunkGroupRenderCuller;
import net.exmo.exworld.world.model.ChunkGroupBounds;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** Keeps the detached camera inside whichever contiguous chunk group currently contains the player. */
final class ChunkGroupCameraBounds {
    private static final double EDGE_MARGIN = 0.35;

    private ChunkGroupCameraBounds() {}

    static double constrainedDistance(Vec3 eye, float yaw, float pitch, double requestedDistance) {
        if (ChunkGroupRenderCuller.bypassBoundary() || !net.exmo.exworld.Config.legacyRegionBoundary) return requestedDistance;
        Vec3 backwards = backwardsDirection(yaw, pitch);
        double maximum = requestedDistance;
        var shape = ClientChunkGroupState.active();
        if (shape == null || !shape.containsPosition(eye.x, eye.z)) {
            int groupChunks = shape == null ? net.exmo.exworld.world.model.WorldDimensions.DEFAULT_GROUP_CHUNKS : shape.groupChunks();
            maximum = limitToCell(eye, backwards, ChunkGroupBounds.containing(eye.x, eye.z, groupChunks), maximum);
        } else {
            // Ray-march over the short camera segment so concave, non-rectangular group borders constrain correctly.
            double safe = 0.0;
            double step = 0.25;
            for (double distance = step; distance <= requestedDistance + step; distance += step) {
                double sample = Math.min(distance, requestedDistance);
                if (!shape.containsPosition(eye.x + backwards.x * sample, eye.z + backwards.z * sample)) {
                    double blocked = sample;
                    for (int iteration = 0; iteration < 6; iteration++) {
                        double middle = (safe + blocked) * 0.5;
                        if (shape.containsPosition(eye.x + backwards.x * middle, eye.z + backwards.z * middle)) safe = middle;
                        else blocked = middle;
                    }
                    break;
                }
                safe = sample;
            }
            maximum = Math.min(maximum, safe);
        }
        return Mth.clamp(maximum, 0.0, requestedDistance);
    }

    private static double limitToCell(Vec3 eye, Vec3 backwards, ChunkGroupBounds bounds, double requestedDistance) {
        double maximum = limitOnAxis(eye.x, backwards.x, bounds.minX() + EDGE_MARGIN,
                bounds.maxX() - EDGE_MARGIN, requestedDistance);
        return limitOnAxis(eye.z, backwards.z, bounds.minZ() + EDGE_MARGIN,
                bounds.maxZ() - EDGE_MARGIN, maximum);
    }

    static Vec3 cameraPosition(Vec3 eye, float yaw, float pitch, double distance) {
        return eye.add(backwardsDirection(yaw, pitch).scale(distance));
    }

    private static Vec3 backwardsDirection(float yaw, float pitch) {
        double yawRadians = Math.toRadians(yaw);
        double pitchRadians = Math.toRadians(pitch);
        double horizontal = Math.cos(pitchRadians);
        return new Vec3(Math.sin(yawRadians) * horizontal, Math.sin(pitchRadians),
                -Math.cos(yawRadians) * horizontal);
    }

    private static double limitOnAxis(double position, double direction, double minimum, double maximum,
                                      double currentLimit) {
        if (direction > 1.0E-6) return Math.min(currentLimit, Math.max(0.0, (maximum - position) / direction));
        if (direction < -1.0E-6) return Math.min(currentLimit, Math.max(0.0, (minimum - position) / direction));
        return currentLimit;
    }
}
