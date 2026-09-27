package net.exmo.exphone.api;

import java.util.List;
import net.minecraft.server.MinecraftServer;

/** Optional NPC directory. The phone has none until another mod connects one. */
public interface PhoneNpcSource {
    List<PhoneNpc> list(MinecraftServer server);

    PhoneNpc find(MinecraftServer server, String id);
}
