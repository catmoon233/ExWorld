package net.exmo.exphone.economy;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.exmo.exphone.api.PhoneEconomyChangedEvent;
import net.exmo.exphone.api.PhoneEconomyMutateEvent;
import net.exmo.exphone.api.PhoneLedger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.NeoForge;

public final class PhoneEconomyApi {
    private static final Map<ResourceLocation, PhoneLedger> connected = new ConcurrentHashMap<>();
    private static final PhoneLedger fallback = new SavedPhoneLedger();

    private PhoneEconomyApi() {}

    public static void connect(ResourceLocation currency, PhoneLedger ledger) {
        if (currency == null || ledger == null) throw new IllegalArgumentException("currency ledger required");
        connected.put(currency, ledger);
    }

    public static long balance(MinecraftServer server, UUID player, ResourceLocation currency) {
        return ledger(currency).balance(server, player, currency);
    }

    public static long add(MinecraftServer server, UUID player, ResourceLocation currency, long amount) {
        long next = mutate(server, player, null, currency, PhoneEconomyMutateEvent.Action.ADD, amount);
        if (next < 0) return balance(server, player, currency);
        long stored = ledger(currency).add(server, player, currency, next);
        changed(server, player, currency, stored);
        return stored;
    }

    public static boolean take(MinecraftServer server, UUID player, ResourceLocation currency, long amount) {
        long next = mutate(server, player, null, currency, PhoneEconomyMutateEvent.Action.TAKE, amount);
        if (next < 0) return false;
        if (next == 0) return true;
        boolean ok = ledger(currency).take(server, player, currency, next);
        if (ok) changed(server, player, currency, balance(server, player, currency));
        return ok;
    }

    public static void set(MinecraftServer server, UUID player, ResourceLocation currency, long amount) {
        long next = mutate(server, player, null, currency, PhoneEconomyMutateEvent.Action.SET, amount);
        if (next < 0) return;
        ledger(currency).set(server, player, currency, next);
        changed(server, player, currency, balance(server, player, currency));
    }

    public static boolean transfer(MinecraftServer server, UUID from, UUID to, ResourceLocation currency, long amount) {
        long next = mutate(server, from, to, currency, PhoneEconomyMutateEvent.Action.TRANSFER, amount);
        if (next < 0) return false;
        if (next == 0) return true;
        boolean ok = ledger(currency).transfer(server, from, to, currency, next);
        if (ok) {
            changed(server, from, currency, balance(server, from, currency));
            changed(server, to, currency, balance(server, to, currency));
        }
        return ok;
    }

    private static PhoneLedger ledger(ResourceLocation currency) {
        return connected.getOrDefault(currency, fallback);
    }

    /** @return adjusted amount, or -1 when cancelled or the request was negative */
    private static long mutate(MinecraftServer server, UUID player, UUID other, ResourceLocation currency,
                               PhoneEconomyMutateEvent.Action action, long amount) {
        if (currency == null || player == null || amount < 0) return -1;
        PhoneEconomyMutateEvent event = new PhoneEconomyMutateEvent(server, player, other, currency, action, amount);
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) return -1;
        return Math.max(0L, event.amount());
    }

    private static void changed(MinecraftServer server, UUID player, ResourceLocation currency, long balance) {
        NeoForge.EVENT_BUS.post(new PhoneEconomyChangedEvent(server, player, currency, balance));
    }
}
