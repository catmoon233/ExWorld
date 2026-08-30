package net.exmo.exworld.client.battle.skybox;

import net.exmo.exworld.battle.api.BattleSnapshot;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;
import java.util.Random;

/** Temporary deterministic-random selector. The battle id keeps all clients in agreement. */
public final class RandomBattleSkyboxSelector implements BattleSkyboxSelector {
    @Override
    public Optional<ResourceLocation> select(BattleSnapshot snapshot, List<ResourceLocation> candidates) {
        if (candidates.isEmpty()) return Optional.empty();
        long seed = snapshot.battleId().value().getMostSignificantBits()
                ^ Long.rotateLeft(snapshot.battleId().value().getLeastSignificantBits(), 19);
        return Optional.of(candidates.get(new Random(seed).nextInt(candidates.size())));
    }
}
