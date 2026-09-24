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

    private static final ModConfigSpec.Builder SERVER_BUILDER = new ModConfigSpec.Builder();
    private static final ModConfigSpec.BooleanValue DECRYPTION_MODE = SERVER_BUILDER
            .comment("Decryption mode. Written only in the server config and synced to connected clients. When enabled, the deck key and party system are hidden, the strategic map starts blank for admin-drawn regions, non-creative players stay in first person, and the backpack hides both weapon rails.")
            .define("gameplay.decryptionMode", false);
    private static final ModConfigSpec.ConfigValue<String> NPC_AI_URL = SERVER_BUILDER
            .comment("OpenAI-compatible chat completions URL for urban NPC dialog. Blank uses each script's fallback line.")
            .define("npc.aiUrl", "");
    private static final ModConfigSpec.ConfigValue<String> NPC_AI_MODEL = SERVER_BUILDER
            .define("npc.aiModel", "gpt-4o-mini");
    private static final ModConfigSpec.IntValue NPC_AI_TIMEOUT = SERVER_BUILDER
            .defineInRange("npc.aiTimeoutMs", 8000, 500, 60000);
    private static final ModConfigSpec.ConfigValue<String> NPC_AI_KEY = SERVER_BUILDER
            .comment("Optional bearer token for urban NPC dialog. Never synced to clients.")
            .define("npc.aiKey", "");
    static final ModConfigSpec SERVER_SPEC = SERVER_BUILDER.build();

    public static boolean debugCardsEnabled = false;
    public static boolean reduceJournalMotion = false;
    public static boolean allowClientGroupEditing = true;
    public static boolean legacyRegionBoundary = false;
    public static boolean autoExpandTiles = false;
    public static boolean defaultArchipelagoPreset = true;
    public static int islandSpacingMin = 200;
    public static int islandSpacingMax = 300;
    public static int zoneTargetSpan = 48;
    public static boolean decryptionMode = false;
    public static String npcAiUrl = "";
    public static String npcAiModel = "gpt-4o-mini";
    public static int npcAiTimeoutMs = 8000;
    public static String npcAiKey = "";
    private static boolean decryptionSyncDirty;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() == SERVER_SPEC) {
            decryptionMode = DECRYPTION_MODE.get();
            npcAiUrl = NPC_AI_URL.get();
            npcAiModel = NPC_AI_MODEL.get();
            npcAiTimeoutMs = NPC_AI_TIMEOUT.get();
            npcAiKey = NPC_AI_KEY.get();
            decryptionSyncDirty = true;
            return;
        }
        if (event.getConfig().getSpec() != SPEC) return;
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

    /** Server tick consumes this once so a config reload reaches every connected client. */
    public static boolean consumeDecryptionSync() {
        if (!decryptionSyncDirty) return false;
        decryptionSyncDirty = false;
        return true;
    }
}
