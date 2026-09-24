package io.redspace.irons_artifice.gun;

import net.minecraft.world.entity.Entity;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Tracks entities damaged during a bullet hit resolution.
 * Seeded with the primary hit target (if any), then filled by on-hit effects that deal damage.
 */
public final class HitEntityAccumulator {
    private final Set<Entity> damaged = new LinkedHashSet<>();

    public boolean add(Entity entity) {
        return damaged.add(entity);
    }

    public boolean contains(Entity entity) {
        return damaged.contains(entity);
    }

    public Set<Entity> all() {
        return Set.copyOf(damaged);
    }

    public boolean isEmpty() {
        return damaged.isEmpty();
    }
}
