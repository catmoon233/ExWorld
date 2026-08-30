package net.exmo.exworld.world.model;

import java.util.List;

public record AnchorSnapshot(List<TravelAnchor> anchors, String currentAnchorId, long cooldownRemainingMs, boolean respawnMode) {
    public AnchorSnapshot { anchors = List.copyOf(anchors); }
}
