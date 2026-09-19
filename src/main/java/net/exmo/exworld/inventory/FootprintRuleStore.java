package net.exmo.exworld.inventory;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLEnvironment;

/** Process-wide footprint table: server SavedData, client replica. */
public final class FootprintRuleStore {
    private static FootprintRules client = FootprintRules.defaults();

    private FootprintRuleStore() {}

    public static FootprintRules current() {
        return FMLEnvironment.dist.isClient() ? client : server(null);
    }

    public static FootprintRules client() {
        return client;
    }

    public static void installClient(FootprintRules rules) {
        client = rules == null ? FootprintRules.defaults() : rules;
    }

    public static FootprintRules server(MinecraftServer server) {
        if (server == null) return FootprintRulesSavedData.fallback();
        return FootprintRulesSavedData.get(server).rules();
    }

    public static void sync(ServerPlayer player) {
        if (player == null || player.getServer() == null) return;
        InventoryNetwork.sendRules(player, server(player.getServer()));
    }
}
