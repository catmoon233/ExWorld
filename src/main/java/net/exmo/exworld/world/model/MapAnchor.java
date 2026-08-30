package net.exmo.exworld.world.model;

/** Client-safe projection of an activated travel anchor for the strategic map. */
public record MapAnchor(String id, String name, int x, int y, int z, String tileId) {}
