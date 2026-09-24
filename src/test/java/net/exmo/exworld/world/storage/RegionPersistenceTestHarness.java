package net.exmo.exworld.world.storage;

import net.exmo.exworld.world.model.Region;

import java.util.List;

/** Guards the group-level settings that must survive SavedData reloads. */
public final class RegionPersistenceTestHarness {
    public static void main(String[] args) {
        Region source = new Region("north", List.of("tile_p0_p0", "tile_p1_p0"), "北境", 42L, "*",
                "风门哨站", "铁矿、松木", true, true);
        Region loaded = WorldNbtCodec.loadRegion(WorldNbtCodec.saveRegion(source));
        require(loaded.equals(source), "group name, icon, site, resources, visibility or leave lock did not survive persistence");
        Region legacy = WorldNbtCodec.loadRegion(WorldNbtCodec.saveRegion(new Region("old", List.of("tile_p0_p0"), "旧区", 1L)));
        require(!legacy.configured() && !legacy.cannotLeave() && legacy.site().isEmpty() && legacy.resources().isEmpty(),
                "unset generated groups must remain hidden and unlocked after persistence");
        System.out.println("REGION_PERSISTENCE_TEST_OK");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
