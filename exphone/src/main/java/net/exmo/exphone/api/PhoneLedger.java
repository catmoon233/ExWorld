package net.exmo.exphone.api;

import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

/** One currency ledger. A connected ledger is only asked about the currency it was connected for. */
public interface PhoneLedger {
    long balance(MinecraftServer server, UUID player, ResourceLocation currency);

    long add(MinecraftServer server, UUID player, ResourceLocation currency, long amount);

    boolean take(MinecraftServer server, UUID player, ResourceLocation currency, long amount);

    void set(MinecraftServer server, UUID player, ResourceLocation currency, long amount);

    default boolean transfer(MinecraftServer server, UUID from, UUID to, ResourceLocation currency, long amount) {
        if (amount < 0 || !take(server, from, currency, amount)) return false;
        add(server, to, currency, amount);
        return true;
    }
}
