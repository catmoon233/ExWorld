package net.exmo.exworld.world.model;

/** Compact map-facing group metadata; tile membership stays in the world snapshot's tiles. */
public record MapRegion(String id, String name, String icon, String site, String resources, boolean configured) {
    public MapRegion {
        icon = icon == null ? "" : icon;
        site = site == null ? "" : site;
        resources = resources == null ? "" : resources;
    }
    public MapRegion(String id, String name, String icon) { this(id, name, icon, "", "", false); }
}
