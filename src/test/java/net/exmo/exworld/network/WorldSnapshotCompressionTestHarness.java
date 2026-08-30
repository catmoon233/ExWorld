package net.exmo.exworld.network;

import net.exmo.exworld.world.generation.WorldLayoutGenerator;
import net.exmo.exworld.world.model.WorldDimensions;
import net.exmo.exworld.world.model.WorldSnapshot;
import net.exmo.exworld.world.model.MapAnchor;
import net.exmo.exworld.world.model.MapTile;
import net.exmo.exworld.world.model.MapRegion;

/** Guards the 128x128 atlas against oversized custom payloads and codec drift. */
public final class WorldSnapshotCompressionTestHarness {
    public static void main(String[] args) {
        var layout = WorldLayoutGenerator.generate(0x4558574F524C44L, 4);
        WorldSnapshot snapshot = new WorldSnapshot(layout.tiles().stream().map(MapTile::from).toList(), "tile_p0_p0", 0,
                layout.tiles().size() * 16, WorldDimensions.MAP_MIN, WorldDimensions.MAP_MIN,
                WorldDimensions.MAP_SIZE, WorldDimensions.MAP_SIZE, 4,
                false, true, java.util.List.of(new MapRegion("biome_group_0", "赤沙港", "@", "赤沙港哨站", "盐、砂岩", true)),
                java.util.List.of(new MapAnchor("anchor_12", "赤沙港锚点", 12, 68, -9, "tile_p0_p0")), 19L);
        byte[] compressed = WorldSnapshotCompression.encode(snapshot);
        WorldSnapshot decoded = WorldSnapshotCompression.decode(compressed);
        require(compressed.length < WorldSnapshotCompression.MAX_COMPRESSED_BYTES, "snapshot exceeds packet budget");
        require(decoded.tiles().size() == 16_384, "tile count changed in codec");
        require(decoded.groupChunks() == 4 && decoded.mapWidth() == 128 && decoded.mapHeight() == 128,
                "dimensions changed in codec");
        require(!decoded.pregenerationEnabled(), "pre-generation state changed in codec");
        require(decoded.manualGroups() && decoded.regions().size() == 1 && decoded.regions().getFirst().icon().equals("@")
                        && decoded.regions().getFirst().configured() && decoded.regions().getFirst().site().equals("赤沙港哨站")
                        && decoded.regions().getFirst().resources().equals("盐、砂岩") && decoded.groupRevision() == 19L,
                "manual-group settings or editor revision changed in codec");
        require(decoded.anchors().size() == 1 && decoded.anchors().getFirst().x() == 12
                && decoded.anchors().getFirst().y() == 68 && decoded.anchors().getFirst().z() == -9
                && decoded.anchors().getFirst().name().equals("赤沙港锚点"), "travel anchor changed in codec");
        require(compressed.length < 130_000, "biome-only map snapshot exceeds the interactive payload budget: " + compressed.length);
        var manualLayout = WorldLayoutGenerator.generate(0x4558574F524C44L, 4, false);
        WorldSnapshot manualSnapshot = new WorldSnapshot(manualLayout.tiles().stream().map(MapTile::from).toList(), "tile_p0_p0", 0,
                manualLayout.tiles().size() * 16, WorldDimensions.MAP_MIN, WorldDimensions.MAP_SIZE, 4,
                false, true, manualLayout.regions().stream().map(region -> new MapRegion(region.id(), region.name(), region.icon())).toList(),
                java.util.List.of());
        int manualBytes = WorldSnapshotCompression.encode(manualSnapshot).length;
        require(manualBytes < WorldSnapshotCompression.MAX_COMPRESSED_BYTES,
                "default manual singleton groups must fit into one editor snapshot packet");
        require(manualBytes < 400_000,
                "default manual singleton groups must remain responsive: " + manualBytes);
        System.out.println("WORLD_SNAPSHOT_COMPRESSION_TEST_OK biomeBytes=" + compressed.length + " manualBytes=" + manualBytes);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
