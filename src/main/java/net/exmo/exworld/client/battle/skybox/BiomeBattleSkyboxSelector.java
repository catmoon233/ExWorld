package net.exmo.exworld.client.battle.skybox;

import net.exmo.exworld.battle.api.BattleSnapshot;
import net.exmo.exworld.world.model.WorldBiome;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Maps the strategic-map biome carried by a battle snapshot to its skybox resource. */
public final class BiomeBattleSkyboxSelector implements BattleSkyboxSelector {
    private static final String SKYBOX_NAMESPACE = "fabricskyboxes";
    private static final String SKYBOX_PREFIX = "sky/exworld/battle/";
    private static final Map<String, String> SKYBOX_BY_BIOME = mappings();

    @Override
    public Optional<ResourceLocation> select(BattleSnapshot snapshot, List<ResourceLocation> candidates) {
        if (snapshot == null || candidates.isEmpty()) return Optional.empty();
        String biomeId = snapshot.biomeId() == null ? "" : snapshot.biomeId().toLowerCase(Locale.ROOT);
        String skyboxName = SKYBOX_BY_BIOME.getOrDefault(biomeId, "prairie");
        ResourceLocation wanted = ResourceLocation.fromNamespaceAndPath(SKYBOX_NAMESPACE, SKYBOX_PREFIX + skyboxName + ".json");
        return candidates.stream().filter(wanted::equals).findFirst()
                .or(() -> candidates.stream().filter(id -> id.getPath().endsWith("/prairie.json")).findFirst())
                .or(() -> candidates.stream().findFirst());
    }

    private static Map<String, String> mappings() {
        Map<String, String> result = new LinkedHashMap<>();
        for (WorldBiome biome : WorldBiome.values()) {
            result.put(biome.id(), biome.id());
            result.put(biome.vanillaBiomeId(), biome.id());
        }
        result.put("minecraft:windswept_forest", "highlands");
        result.put("minecraft:windswept_gravelly_hills", "highlands");
        return Map.copyOf(result);
    }
}
