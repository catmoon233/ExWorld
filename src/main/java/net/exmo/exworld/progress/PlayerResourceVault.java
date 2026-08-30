package net.exmo.exworld.progress;

import java.util.*;
import net.exmo.exworld.Exworld;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

/** Server-authoritative multi-resource ledger. All mutations are non-negative, saturated and persistent. */
public final class PlayerResourceVault {
    public static final ResourceLocation GOLD = ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "gold");
    private static final String DATA_NAME = "exworld_player_progress";
    public long balance(MinecraftServer server, UUID player, ResourceLocation resource) { return state(server, player).resources.getOrDefault(resource, 0L); }
    public Map<ResourceLocation, Long> balances(MinecraftServer server, UUID player) { return Map.copyOf(state(server, player).resources); }
    public void set(MinecraftServer server, UUID player, ResourceLocation resource, long amount) { state(server, player).resources.put(resource, Math.max(0L, amount)); saved(server).changed(); }
    public long add(MinecraftServer server, UUID player, ResourceLocation resource, long amount) {
        if (amount <= 0) return balance(server, player, resource); PlayerProgress state = state(server, player); long current = state.resources.getOrDefault(resource, 0L); long next = current >= Long.MAX_VALUE - amount ? Long.MAX_VALUE : current + amount; state.resources.put(resource, next); saved(server).changed(); return next;
    }
    public boolean take(MinecraftServer server, UUID player, ResourceLocation resource, long amount) {
        if (amount < 0) return false; PlayerProgress state = state(server, player); long current = state.resources.getOrDefault(resource, 0L); if (current < amount) return false; state.resources.put(resource, current - amount); saved(server).changed(); return true;
    }
    public boolean transfer(MinecraftServer server, UUID from, UUID to, ResourceLocation resource, long amount) {
        if (amount < 0 || !take(server, from, resource, amount)) return false; add(server, to, resource, amount); return true;
    }
    /** Returns false when the same durable source was already awarded. */
    public boolean awardOnce(MinecraftServer server, UUID player, String source, ResourceLocation resource, long amount) {
        PlayerProgress state = state(server, player); if (!state.processedSources.add("resource:" + source)) return false; if (amount > 0) { long current = state.resources.getOrDefault(resource, 0L); state.resources.put(resource, current >= Long.MAX_VALUE - amount ? Long.MAX_VALUE : current + amount); } saved(server).changed(); return true;
    }
    public PlayerProgress state(MinecraftServer server, UUID player) { return saved(server).player(player); }
    public PlayerProgressSavedData saved(MinecraftServer server) { return server.overworld().getDataStorage().computeIfAbsent(PlayerProgressSavedData.FACTORY, DATA_NAME); }
}
