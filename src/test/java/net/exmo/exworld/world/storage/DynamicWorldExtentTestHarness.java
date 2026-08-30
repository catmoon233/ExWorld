package net.exmo.exworld.world.storage;

import net.exmo.exworld.world.model.WorldDimensions;
import net.exmo.exworld.world.model.ManualChunkGroupLayout;

import java.util.List;

/** Regression harness for on-demand cells beyond the original 128×128 client view window. */
public final class DynamicWorldExtentTestHarness {
    public static void main(String[] args) {
        WorldStateData state = new WorldStateData();
        state.initialize(0x4558574F524C44L);
        int initialTiles = state.tiles().size();
        long initialRevision = state.groupRevision();
        var expanded = state.ensureTile(512, -384).orElseThrow();
        require(state.tiles().size() == initialTiles + 1, "new remote world tile was not persisted");
        require(expanded.mapX() == 512 && expanded.mapZ() == -384 && !state.region(expanded.regionId()).orElseThrow().configured(),
                "expanded tile must be an unconfigured editable singleton group");
        require(state.ensureTile(512, -384).orElseThrow().id().equals(expanded.id()) && state.tiles().size() == initialTiles + 1,
                "materializing the same coordinate must be idempotent");
        require(state.groupRevision() == initialRevision + 1, "only a new tile may advance the collaborative revision");
        state.applyGroupEdit(0x4558574F524C44L, true, List.of(new ManualChunkGroupLayout.Group("remote_outpost", "远方前哨", "@",
                "远方哨站", "铁矿、药草", true, List.of(expanded.id()))));
        require(state.tile(expanded.id()).orElseThrow().regionId().equals("remote_outpost")
                        && state.region("remote_outpost").orElseThrow().configured(),
                "windowed edit must update only its loaded remote group without replacing the initial world");
        int maximum = WorldDimensions.maximumGroupCoordinate(state.groupChunks());
        require(state.ensureTile(maximum + 1, 0).isEmpty(), "tile beyond the maximum Minecraft border was accepted");
        System.out.println("DYNAMIC_WORLD_EXTENT_TEST_OK tile=" + expanded.id());
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
