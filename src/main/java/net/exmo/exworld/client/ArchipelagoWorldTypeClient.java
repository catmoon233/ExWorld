package net.exmo.exworld.client;

import net.exmo.exworld.Config;
import net.exmo.exworld.Exworld;
import net.exmo.exworld.world.generation.ArchipelagoPresets;
import net.exmo.exworld.world.generation.IslandLayout;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.WeakHashMap;

/** Selects 浮穹群岛 on the create-world screen once, without locking the dropdown afterwards. */
@EventBusSubscriber(modid = Exworld.MODID, value = Dist.CLIENT)
public final class ArchipelagoWorldTypeClient {
    private static final WeakHashMap<CreateWorldScreen, Boolean> APPLIED = new WeakHashMap<>();

    private ArchipelagoWorldTypeClient() {}

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof CreateWorldScreen screen)) return;
        WorldCreationUiState state = screen.getUiState();
        WorldOptions.parseSeed(state.getSeed()).ifPresent(IslandLayout::bindSeed);
        if (!Config.defaultArchipelagoPreset) return;
        if (APPLIED.putIfAbsent(screen, Boolean.TRUE) != null) return;
        for (WorldCreationUiState.WorldTypeEntry entry : state.getNormalPresetList()) {
            if (entry.preset() != null && entry.preset().is(ArchipelagoPresets.ARCHIPELAGO)) {
                state.setWorldType(entry);
                return;
            }
        }
    }
}
