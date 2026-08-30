package net.exmo.exworld.world.storage;

import net.minecraft.server.MinecraftServer;

public final class NeoForgeWorldStateStore implements WorldStateStore {
    @Override
    public WorldStateData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(WorldStateData.FACTORY, "exworld_world_state");
    }
}
