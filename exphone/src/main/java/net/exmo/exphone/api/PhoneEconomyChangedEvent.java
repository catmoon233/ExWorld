package net.exmo.exphone.api;

import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.Event;

/** Fired after a currency change has been stored. */
public final class PhoneEconomyChangedEvent extends Event {
    private final MinecraftServer server;
    private final UUID player;
    private final ResourceLocation currency;
    private final long balance;

    public PhoneEconomyChangedEvent(MinecraftServer server, UUID player, ResourceLocation currency, long balance) {
        this.server = server;
        this.player = player;
        this.currency = currency;
        this.balance = balance;
    }

    public MinecraftServer server() { return server; }
    public UUID player() { return player; }
    public ResourceLocation currency() { return currency; }
    public long balance() { return balance; }
}
