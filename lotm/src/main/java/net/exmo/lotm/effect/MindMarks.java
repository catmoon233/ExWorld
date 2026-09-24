package net.exmo.lotm.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Short-lived caster link for guidance and mimic. Cleared when the effect ends. */
public final class MindMarks {
    public enum Kind {
        REDIRECT,
        FOLLOW,
        MIMIC
    }

    private static final Map<UUID, UUID> CASTERS = new ConcurrentHashMap<>();

    private MindMarks() {}

    public static void mark(LivingEntity target, LivingEntity caster) {
        if (target == null || caster == null) return;
        CASTERS.put(target.getUUID(), caster.getUUID());
    }

    public static void clear(LivingEntity target) {
        if (target != null) CASTERS.remove(target.getUUID());
    }

    public static LivingEntity caster(LivingEntity target) {
        if (target == null || !(target.level() instanceof ServerLevel level)) return null;
        UUID id = CASTERS.get(target.getUUID());
        if (id == null) return null;
        Entity entity = level.getEntity(id);
        return entity instanceof LivingEntity living && living.isAlive() ? living : null;
    }
}
