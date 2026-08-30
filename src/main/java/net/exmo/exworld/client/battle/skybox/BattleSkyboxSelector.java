package net.exmo.exworld.client.battle.skybox;

import net.exmo.exworld.battle.api.BattleSnapshot;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

/**
 * Selects the skybox for a battle from the candidates exposed by the optional
 * skybox adapter. This is the seam for future rules such as arena or
 * encounter-driven selection.
 */
@FunctionalInterface
public interface BattleSkyboxSelector {
    Optional<ResourceLocation> select(BattleSnapshot snapshot, List<ResourceLocation> candidates);
}
