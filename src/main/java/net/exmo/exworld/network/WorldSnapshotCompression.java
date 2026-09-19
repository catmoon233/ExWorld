package net.exmo.exworld.network;

import net.exmo.exworld.world.model.WorldSnapshot;
import net.exmo.exworld.world.model.MapTile;
import net.exmo.exworld.world.model.MapAnchor;
import net.exmo.exworld.world.model.MapRegion;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/** Hides the bounded dynamic-map-window wire format behind one compressed snapshot interface. */
final class WorldSnapshotCompression {
    static final int MAX_COMPRESSED_BYTES = 1_048_576;
    private static final int MAX_TILES = 128 * 128;

    private WorldSnapshotCompression() {}

    static byte[] encode(WorldSnapshot snapshot) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(new GZIPOutputStream(bytes))) {
                output.writeInt(snapshot.tiles().size());
                for (MapTile tile : snapshot.tiles()) writeTile(output, tile);
                output.writeUTF(snapshot.currentTileId());
                output.writeInt(snapshot.generatedChunks());
                output.writeInt(snapshot.totalChunks());
                output.writeInt(snapshot.mapMinimumX());
                output.writeInt(snapshot.mapMinimumZ());
                output.writeInt(snapshot.mapWidth());
                output.writeInt(snapshot.mapHeight());
                output.writeInt(snapshot.groupChunks());
                output.writeBoolean(snapshot.pregenerationEnabled());
                output.writeBoolean(snapshot.manualGroups());
                output.writeInt(snapshot.regions().size());
                for (MapRegion region : snapshot.regions()) {
                    output.writeUTF(region.id()); output.writeUTF(region.name()); output.writeUTF(region.icon());
                    output.writeUTF(region.site()); output.writeUTF(region.resources()); output.writeBoolean(region.configured());
                }
                output.writeInt(snapshot.anchors().size());
                for (MapAnchor anchor : snapshot.anchors()) {
                    output.writeUTF(anchor.id()); output.writeUTF(anchor.name());
                    output.writeInt(anchor.x()); output.writeInt(anchor.y()); output.writeInt(anchor.z());
                    output.writeUTF(anchor.tileId());
                }
                output.writeLong(snapshot.groupRevision());
                output.writeBoolean(snapshot.archipelago());
            }
            byte[] compressed = bytes.toByteArray();
            if (compressed.length > MAX_COMPRESSED_BYTES) {
                throw new IllegalArgumentException("compressed world snapshot exceeds " + MAX_COMPRESSED_BYTES + " bytes");
            }
            return compressed;
        } catch (IOException exception) {
            throw new IllegalStateException("could not encode world snapshot", exception);
        }
    }

    static WorldSnapshot decode(byte[] compressed) {
        if (compressed.length > MAX_COMPRESSED_BYTES) throw new IllegalArgumentException("world snapshot is too large");
        try (DataInputStream input = new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(compressed)))) {
            int count = bounded(input.readInt(), 0, MAX_TILES, "tile count");
            List<MapTile> tiles = new ArrayList<>(count);
            for (int i = 0; i < count; i++) tiles.add(readTile(input));
            String currentTile = input.readUTF();
            int generated = input.readInt();
            int total = input.readInt();
            int mapMinimumX = input.readInt();
            int mapMinimumZ = input.readInt();
            int mapWidth = bounded(input.readInt(), 1, 512, "map width");
            int mapHeight = bounded(input.readInt(), 1, 512, "map height");
            int groupChunks = input.readInt();
            boolean pregenerationEnabled = input.readBoolean();
            boolean manualGroups = input.readBoolean();
            int regionCount = bounded(input.readInt(), 0, MAX_TILES, "region count");
            List<MapRegion> regions = new ArrayList<>(regionCount);
            for (int i = 0; i < regionCount; i++) regions.add(new MapRegion(input.readUTF(), input.readUTF(), input.readUTF(),
                    input.readUTF(), input.readUTF(), input.readBoolean()));
            int anchorCount = bounded(input.readInt(), 0, 4096, "anchor count");
            List<MapAnchor> anchors = new ArrayList<>(anchorCount);
            for (int i = 0; i < anchorCount; i++) {
                anchors.add(new MapAnchor(input.readUTF(), input.readUTF(), input.readInt(), input.readInt(),
                        input.readInt(), input.readUTF()));
            }
            long groupRevision = input.readLong();
            boolean archipelago = input.readBoolean();
            return new WorldSnapshot(tiles, currentTile, generated, total, mapMinimumX, mapMinimumZ, mapWidth, mapHeight,
                    groupChunks, pregenerationEnabled, manualGroups, regions, anchors, groupRevision, archipelago);
        } catch (IOException exception) {
            throw new IllegalArgumentException("could not decode world snapshot", exception);
        }
    }
    private static void writeTile(DataOutputStream output, MapTile tile) throws IOException {
        output.writeUTF(tile.id());
        output.writeInt(tile.mapX()); output.writeInt(tile.mapZ());
        output.writeUTF(tile.regionId()); output.writeUTF(tile.biomeId()); output.writeUTF(tile.sites());
    }

    private static MapTile readTile(DataInputStream input) throws IOException {
        return new MapTile(input.readUTF(), input.readInt(), input.readInt(), input.readUTF(), input.readUTF(), input.readUTF());
    }

    private static int bounded(int value, int minimum, int maximum, String name) throws IOException {
        if (value < minimum || value > maximum) throw new IOException(name + " outside bounds: " + value);
        return value;
    }
}
