package net.exmo.exworld;

import net.exmo.exworld.battle.BattleSystem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = Exworld.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.BooleanValue DEBUG_CARDS = BUILDER
            .comment("Register the old debug card catalog for development commands. Disabled by default; restart after changing.")
            .define("battle.debugCards", false);
    private static final ModConfigSpec.BooleanValue REDUCE_JOURNAL_MOTION = BUILDER
            .comment("Reduce page, reward and mail animations in the ExWorld adventure journal.")
            .define("ui.reduceJournalMotion", false);
    private static final ModConfigSpec.BooleanValue ALLOW_CLIENT_GROUP_EDITING = BUILDER
            .comment("Allow connected clients to open and save the collaborative world-group editor. Disable to restrict it to server-side administration.")
            .define("world.allowClientGroupEditing", true);
    private static final ModConfigSpec.BooleanValue LEGACY_REGION_BOUNDARY = BUILDER
            .comment("Restore the original full-height region wall, hide chunks/entities outside the active region, and clamp the camera. Off by default: a short translucent wall with no culling.")
            .define("world.legacyRegionBoundary", false);
    private static final ModConfigSpec.BooleanValue AUTO_EXPAND_TILES = BUILDER
            .comment("When walking past the initial 128x128 atlas, create new world tiles on demand. Off by default so the atlas edge is a hard boundary.")
            .define("world.autoExpandTiles", false);
    private static final ModConfigSpec.BooleanValue DEFAULT_ARCHIPELAGO_PRESET = BUILDER
            .comment("Select the 浮穹群岛 / Archipelago world type when opening the create-world screen.")
            .define("world.defaultArchipelagoPreset", true);
    private static final ModConfigSpec.IntValue ISLAND_SPACING_MIN = BUILDER
            .comment("Minimum centre-to-centre distance between floating islands, in blocks. Requires a world restart.")
            .worldRestart()
            .defineInRange("world.islandSpacingMin", 200, 64, 1024);
    private static final ModConfigSpec.IntValue ISLAND_SPACING_MAX = BUILDER
            .comment("Maximum centre-to-centre distance between floating islands, in blocks. Requires a world restart.")
            .worldRestart()
            .defineInRange("world.islandSpacingMax", 300, 64, 1024);
    private static final ModConfigSpec.IntValue ZONE_TARGET_SPAN = BUILDER
            .comment("Target side length of a named biome zone on the strategic map, in world tiles. 48 tiles is about 3000 blocks at the default group size.")
            .worldRestart()
            .defineInRange("world.zoneTargetSpan", 48, 8, 128);

    static final ModConfigSpec SPEC = BUILDER.build();
    public static boolean debugCardsEnabled = false;
    public static boolean reduceJournalMotion = false;
    public static boolean allowClientGroupEditing = true;
    public static boolean legacyRegionBoundary = false;
    public static boolean autoExpandTiles = false;
    public static boolean defaultArchipelagoPreset = true;
    public static int islandSpacingMin = 200;
    public static int islandSpacingMax = 300;
    public static int zoneTargetSpan = 48;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        debugCardsEnabled = DEBUG_CARDS.get();
        reduceJournalMotion = REDUCE_JOURNAL_MOTION.get();
        allowClientGroupEditing = ALLOW_CLIENT_GROUP_EDITING.get();
        legacyRegionBoundary = LEGACY_REGION_BOUNDARY.get();
        autoExpandTiles = AUTO_EXPAND_TILES.get();
        defaultArchipelagoPreset = DEFAULT_ARCHIPELAGO_PRESET.get();
        islandSpacingMin = ISLAND_SPACING_MIN.get();
        islandSpacingMax = ISLAND_SPACING_MAX.get();
        zoneTargetSpan = ZONE_TARGET_SPAN.get();
        if (event instanceof ModConfigEvent.Loading && debugCardsEnabled) {
            BattleSystem.registerDebugCards();
        }
    }
}
