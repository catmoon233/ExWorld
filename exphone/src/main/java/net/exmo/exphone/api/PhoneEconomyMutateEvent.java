package net.exmo.exphone.api;

import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/** Fired before a currency change. Listeners may change the amount or cancel the change. */
public final class PhoneEconomyMutateEvent extends Event implements ICancellableEvent {
    public enum Action { ADD, TAKE, SET, TRANSFER }

    private final MinecraftServer server;
    private final UUID player;
    private final UUID other;
    private final ResourceLocation currency;
    private final Action action;
    private long amount;

    public PhoneEconomyMutateEvent(MinecraftServer server, UUID player, UUID other, ResourceLocation currency, Action action, long amount) {
        this.server = server;
        this.player = player;
        this.other = other;
        this.currency = currency;
        this.action = action;
        this.amount = amount;
    }

    public MinecraftServer server() { return server; }
    public UUID player() { return player; }
    /** Transfer recipient. Null for add, take and set. */
    public UUID other() { return other; }
    public ResourceLocation currency() { return currency; }
    public Action action() { return action; }
    public long amount() { return amount; }
    public void setAmount(long amount) { this.amount = amount; }
}
