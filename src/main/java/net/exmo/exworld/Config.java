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

    static final ModConfigSpec SPEC = BUILDER.build();
    public static boolean debugCardsEnabled = false;
    public static boolean reduceJournalMotion = false;
    public static boolean allowClientGroupEditing = true;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        debugCardsEnabled = DEBUG_CARDS.get();
        reduceJournalMotion = REDUCE_JOURNAL_MOTION.get();
        allowClientGroupEditing = ALLOW_CLIENT_GROUP_EDITING.get();
        if (event instanceof ModConfigEvent.Loading && debugCardsEnabled) {
            BattleSystem.registerDebugCards();
        }
    }
}
