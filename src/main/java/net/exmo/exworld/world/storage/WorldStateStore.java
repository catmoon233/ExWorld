package net.exmo.exworld.world.storage;

import net.minecraft.server.MinecraftServer;

/** Persistence seam. NeoForge uses SavedData; a Fabric build can supply a CCA adapter. */
public interface WorldStateStore {
    WorldStateData get(MinecraftServer server);
}
