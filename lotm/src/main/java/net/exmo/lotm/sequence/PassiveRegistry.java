package net.exmo.lotm.sequence;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class PassiveRegistry {
    private static final Map<ResourceLocation, PassiveDefinition> PASSIVES = new LinkedHashMap<>();

    private PassiveRegistry() {}

    public static void register(PassiveDefinition definition) {
        if (definition == null) return;
        PASSIVES.put(definition.id(), definition);
    }

    public static Optional<PassiveDefinition> get(ResourceLocation id) {
        return Optional.ofNullable(PASSIVES.get(id));
    }

    public static Collection<PassiveDefinition> all() {
        return PASSIVES.values();
    }
}
