package net.exmo.exworld.monster;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

/** Selects an entity type exactly, or every entity in an entity-type tag. */
public record MonsterSelector(Kind kind, String id) {
    public enum Kind { ENTITY_TYPE, ENTITY_TAG }
    public MonsterSelector {
        if (kind == null || id == null || id.isBlank()) throw new IllegalArgumentException("selector is required");
        ResourceLocation.parse(id.startsWith("#") ? id.substring(1) : id);
    }
    public boolean matches(EntityType<?> type) {
        ResourceLocation key = ResourceLocation.parse(id.startsWith("#") ? id.substring(1) : id);
        return switch (kind) {
            case ENTITY_TYPE -> key.equals(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(type));
            case ENTITY_TAG -> type.builtInRegistryHolder().is(TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, key));
        };
    }
}
