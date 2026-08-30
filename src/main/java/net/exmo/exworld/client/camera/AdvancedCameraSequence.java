/*
 * Adapted from StarRailExpress2, GPL-3.0-or-later. See THIRD_PARTY_NOTICES.md.
 */
package net.exmo.exworld.client.camera;

import java.util.List;

public record AdvancedCameraSequence(List<AdvancedCameraNode> nodes, boolean blackBars) {
    public AdvancedCameraSequence { nodes = List.copyOf(nodes); }
    public int totalTicks() { return nodes.stream().mapToInt(AdvancedCameraNode::totalTicks).sum(); }
}
