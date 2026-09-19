package net.exmo.exworld.ship;

import net.exmo.exworld.ship.model.ShipBlock;
import net.exmo.exworld.ship.model.ShipHull;
import net.exmo.exworld.ship.model.ShipSlot;
import net.exmo.exworld.ship.storage.ShipNbtCodec;

import java.util.ArrayList;
import java.util.List;

/** Palette round-trip and volume limit for 船体结构. */
public final class ShipHullCodecTestHarness {
    public static void main(String[] args) {
        ShipHull hull = ShipHull.builder()
                .add(3, 1, 4, "minecraft:oak_planks")
                .add(4, 1, 4, "minecraft:oak_planks")
                .add(3, 2, 4, "minecraft:chest", List.of(ShipSlot.of(0, "minecraft:apple", 3)))
                .add(5, 1, 4, "minecraft:oak_door[open=false]")
                .build();
        require(hull.size() == 4, "capture should keep four voxels");
        require(hull.sizeX() == 3 && hull.sizeY() == 2 && hull.sizeZ() == 1, "origin should remap to zero");
        require(hull.blockAt(0, 0, 0).orElse("").equals("minecraft:oak_planks"), "min corner should become origin");
        require(hull.blocks().stream().anyMatch(ShipBlock::hasContainer), "non-empty chest slot should persist");
        require(hull.palette().size() == 3, "identical planks should share a palette entry");

        byte[] encoded = ShipNbtCodec.encodeHull(hull);
        require(encoded.length < ShipNbtCodec.MAX_COMPRESSED_BYTES, "compressed hull exceeds packet budget");
        ShipHull decoded = ShipNbtCodec.decodeHull(encoded);
        require(decoded.size() == hull.size(), "voxel count changed in codec");
        require(decoded.revision() == hull.revision(), "revision changed in codec");
        require(decoded.blockAt(0, 1, 0).orElse("").equals("minecraft:chest"), "chest palette key lost");
        require(decoded.containerAt(decoded.blocks().stream().filter(ShipBlock::hasContainer).findFirst().orElseThrow().packed())
                .getFirst().itemId().equals("minecraft:apple"), "container extra lost");

        List<ShipBlock> tooMany = new ArrayList<>();
        for (int i = 0; i < ShipHull.MAX_BLOCKS + 1; i++) {
            tooMany.add(new ShipBlock(i % 64, (i / 64) % 64, i / 4096, "minecraft:stone", List.of()));
        }
        boolean rejected = false;
        try { ShipHull.of(tooMany); } catch (IllegalArgumentException exception) { rejected = true; }
        require(rejected, "hull must reject more than " + ShipHull.MAX_BLOCKS + " blocks");
        System.out.println("SHIP_HULL_CODEC_TEST_OK");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
