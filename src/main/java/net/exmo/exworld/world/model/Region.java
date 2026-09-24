package net.exmo.exworld.world.model;

import java.util.List;

/** A named story-facing place composed of one or more connected world tiles and its map-visible group settings. */
public record Region(String id, List<String> tileIds, String name, long storySeed, String icon,
                     String site, String resources, boolean configured, boolean cannotLeave) {
    public Region { tileIds = List.copyOf(tileIds); }
    public Region(String id, List<String> tileIds, String name, long storySeed, String icon,
                  String site, String resources, boolean configured) {
        this(id, tileIds, name, storySeed, icon, site, resources, configured, false);
    }
    public Region(String id, List<String> tileIds, String name, long storySeed, String icon) {
        this(id, tileIds, name, storySeed, icon, "", "", false, false);
    }
    public Region(String id, List<String> tileIds, String name, long storySeed) {
        this(id, tileIds, name, storySeed, "", "", "", false, false);
    }
    public Region(String id, String tileId, String name, long storySeed) {
        this(id, List.of(tileId), name, storySeed, "", "", "", false, false);
    }
}
