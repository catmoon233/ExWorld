package net.exmo.exphone.economy;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.exmo.exphone.api.PhoneLedger;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Built-in ledger used for every currency another mod has not connected. */
public final class SavedPhoneLedger implements PhoneLedger {
    @Override
    public long balance(MinecraftServer server, UUID player, ResourceLocation currency) {
        return data(server).balance(player, currency);
    }

    @Override
    public long add(MinecraftServer server, UUID player, ResourceLocation currency, long amount) {
        if (amount <= 0) return balance(server, player, currency);
        EconomySavedData saved = data(server);
        long current = saved.balance(player, currency);
        long next = current >= Long.MAX_VALUE - amount ? Long.MAX_VALUE : current + amount;
        saved.set(player, currency, next);
        return next;
    }

    @Override
    public boolean take(MinecraftServer server, UUID player, ResourceLocation currency, long amount) {
        if (amount < 0) return false;
        EconomySavedData saved = data(server);
        long current = saved.balance(player, currency);
        if (current < amount) return false;
        saved.set(player, currency, current - amount);
        return true;
    }

    @Override
    public void set(MinecraftServer server, UUID player, ResourceLocation currency, long amount) {
        data(server).set(player, currency, Math.max(0L, amount));
    }

    private static EconomySavedData data(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(EconomySavedData.FACTORY, EconomySavedData.ID);
    }

    static final class EconomySavedData extends SavedData {
        static final String ID = "exphone_economy";
        static final Factory<EconomySavedData> FACTORY = new Factory<>(EconomySavedData::new, EconomySavedData::load);
        private final Map<UUID, Map<String, Long>> balances = new HashMap<>();

        long balance(UUID player, ResourceLocation currency) {
            Map<String, Long> owned = balances.get(player);
            return owned == null ? 0L : owned.getOrDefault(key(currency), 0L);
        }

        void set(UUID player, ResourceLocation currency, long amount) {
            balances.computeIfAbsent(player, ignored -> new HashMap<>()).put(key(currency), Math.max(0L, amount));
            setDirty();
        }

        private static String key(ResourceLocation currency) {
            return currency.toString();
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            ListTag players = new ListTag();
            balances.forEach((id, amounts) -> {
                CompoundTag player = new CompoundTag();
                player.putUUID("id", id);
                CompoundTag stored = new CompoundTag();
                amounts.forEach(stored::putLong);
                player.put("balances", stored);
                players.add(player);
            });
            tag.put("players", players);
            return tag;
        }

        private static EconomySavedData load(CompoundTag tag, HolderLookup.Provider registries) {
            EconomySavedData data = new EconomySavedData();
            ListTag players = tag.getList("players", Tag.TAG_COMPOUND);
            for (int i = 0; i < players.size(); i++) {
                CompoundTag player = players.getCompound(i);
                Map<String, Long> amounts = new HashMap<>();
                CompoundTag stored = player.getCompound("balances");
                for (String key : stored.getAllKeys()) amounts.put(key, stored.getLong(key));
                data.balances.put(player.getUUID("id"), amounts);
            }
            return data;
        }
    }
}
