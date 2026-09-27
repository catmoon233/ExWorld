package net.exmo.exphone.api;

import java.util.UUID;
import net.exmo.exphone.ExPhone;
import net.exmo.exphone.economy.PhoneEconomyApi;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

/**
 * Phone gold and any other currency another mod connects.
 * Call {@link #connect} to replace storage for one currency id. Listen to
 * {@link PhoneEconomyMutateEvent} to change or cancel an amount without owning the ledger.
 */
public final class PhoneEconomy {
    public static final ResourceLocation GOLD = ResourceLocation.fromNamespaceAndPath(ExPhone.MODID, "gold");

    private PhoneEconomy() {}

    public static void connect(ResourceLocation currency, PhoneLedger ledger) {
        PhoneEconomyApi.connect(currency, ledger);
    }

    public static long balance(MinecraftServer server, UUID player, ResourceLocation currency) {
        return PhoneEconomyApi.balance(server, player, currency);
    }

    public static long add(MinecraftServer server, UUID player, ResourceLocation currency, long amount) {
        return PhoneEconomyApi.add(server, player, currency, amount);
    }

    public static boolean take(MinecraftServer server, UUID player, ResourceLocation currency, long amount) {
        return PhoneEconomyApi.take(server, player, currency, amount);
    }

    public static void set(MinecraftServer server, UUID player, ResourceLocation currency, long amount) {
        PhoneEconomyApi.set(server, player, currency, amount);
    }

    public static boolean transfer(MinecraftServer server, UUID from, UUID to, ResourceLocation currency, long amount) {
        return PhoneEconomyApi.transfer(server, from, to, currency, amount);
    }
}
