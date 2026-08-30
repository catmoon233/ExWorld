package net.exmo.exworld.client.battle.skybox;

import net.exmo.exworld.Exworld;
import net.exmo.exworld.battle.api.BattleSnapshot;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Owns the battle lifecycle and keeps selection policy separate from mod compatibility. */
public final class BattleSkyboxController {
    private static final String OPTIONAL_BRIDGE =
            "net.exmo.exworld.client.battle.skybox.NeoforgeSkyboxesBattleBridge";
    private static volatile BattleSkyboxSelector selector = new BiomeBattleSkyboxSelector();
    private static final BattleSkyboxBridge BRIDGE = loadBridge();
    private static UUID activeBattle;
    private static BattleSnapshot activeSnapshot;
    private static ResourceLocation selectedSkybox;

    private BattleSkyboxController() {}

    public static void onBattleStarted(BattleSnapshot snapshot) {
        if (snapshot == null || snapshot.battleId() == null) return;
        UUID battleId = snapshot.battleId().value();
        if (battleId.equals(activeBattle)) return;

        if (activeBattle != null) BRIDGE.deactivate();
        activeBattle = battleId;
        activeSnapshot = snapshot;
        selectedSkybox = null;
        selectAndActivate(snapshot);
    }

    public static void onBattleEnded() {
        if (activeBattle == null) return;
        BRIDGE.deactivate();
        activeBattle = null;
        activeSnapshot = null;
        selectedSkybox = null;
    }

    /** Replace the default biome policy with an arena/encounter-aware policy. */
    public static void setSelector(BattleSkyboxSelector nextSelector) {
        selector = nextSelector == null ? new BiomeBattleSkyboxSelector() : nextSelector;
    }

    public static void tick() {
        if (activeBattle == null) return;
        if (selectedSkybox == null && activeSnapshot != null) selectAndActivate(activeSnapshot);
        BRIDGE.tick();
    }

    private static void selectAndActivate(BattleSnapshot snapshot) {
        List<ResourceLocation> candidates = BRIDGE.candidates();
        Optional<ResourceLocation> selected = selector.select(snapshot, candidates);
        selected.ifPresent(value -> {
            selectedSkybox = value;
            BRIDGE.activate(value);
        });
    }

    private static BattleSkyboxBridge loadBridge() {
        try {
            return (BattleSkyboxBridge) Class.forName(OPTIONAL_BRIDGE)
                    .getDeclaredConstructor().newInstance();
        } catch (Throwable error) {
            Exworld.LOGGER.debug("NeoforgeSkyboxes is not available; battle skyboxes are disabled");
            return new BattleSkyboxBridge() {
                @Override public List<ResourceLocation> candidates() { return List.of(); }
                @Override public void activate(ResourceLocation skyboxId) {}
                @Override public void deactivate() {}
            };
        }
    }
}
