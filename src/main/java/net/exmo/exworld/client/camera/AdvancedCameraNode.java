/*
 * Adapted from StarRailExpress2, GPL-3.0-or-later. See THIRD_PARTY_NOTICES.md.
 */
package net.exmo.exworld.client.camera;

import net.minecraft.world.phys.Vec3;

public record AdvancedCameraNode(int durationTicks, int holdTicks, Vec3 position, Vec3 lookAt, float fov) {
    public AdvancedCameraNode { durationTicks = Math.max(0, durationTicks); holdTicks = Math.max(0, holdTicks); }
    public int totalTicks() { return durationTicks + holdTicks; }
}
