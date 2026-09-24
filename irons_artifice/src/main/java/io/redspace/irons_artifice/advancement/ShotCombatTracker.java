package io.redspace.irons_artifice.advancement;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Transient per-player counters for pellet hits and accumulated damage (by fire id) and unique kills (by lineage id).
 */
public final class ShotCombatTracker {
    private static final int MAX_ENTRIES = 48;

    private final Map<UUID, Int2IntOpenHashMap> pelletsByFire = new HashMap<>();
    private final Map<UUID, Set<UUID>> killsByLineage = new HashMap<>();
    private final Map<UUID, Float> damageByFire = new HashMap<>();
    private long instaReloadTick = Long.MIN_VALUE;

    public int recordPelletHit(UUID fireId, int entityId) {
        prune(pelletsByFire);
        Int2IntOpenHashMap perEntity = pelletsByFire.computeIfAbsent(fireId, id -> new Int2IntOpenHashMap());
        int next = perEntity.get(entityId) + 1;
        perEntity.put(entityId, next);
        return next;
    }

    public int recordKill(UUID lineageId, UUID victimId) {
        prune(killsByLineage);
        Set<UUID> victims = killsByLineage.computeIfAbsent(lineageId, id -> new HashSet<>());
        victims.add(victimId);
        return victims.size();
    }

    public float recordDamage(UUID fireId, float amount) {
        prune(damageByFire);
        float next = damageByFire.getOrDefault(fireId, 0f) + amount;
        damageByFire.put(fireId, next);
        return next;
    }

    public void markInstaReload(long gameTime) {
        this.instaReloadTick = gameTime;
    }

    public boolean instaReloaded(long gameTime) {
        return instaReloadTick != Long.MIN_VALUE && gameTime - instaReloadTick <= 100;
    }

    private static void prune(Map<UUID, ?> map) {
        if (map.size() <= MAX_ENTRIES) {
            return;
        }
        Iterator<UUID> iterator = map.keySet().iterator();
        while (map.size() > MAX_ENTRIES / 2 && iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }
    }
}
