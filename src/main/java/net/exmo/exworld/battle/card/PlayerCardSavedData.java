package net.exmo.exworld.battle.card;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

/** World-scoped card ownership with idempotent v1 migration. */
public final class PlayerCardSavedData extends SavedData {
    public static final Factory<PlayerCardSavedData> FACTORY = new Factory<>(PlayerCardSavedData::new, PlayerCardSavedData::load);
    private final Map<UUID, PlayerCardCollection> players = new LinkedHashMap<>();
    private final Set<String> processedRewards = new LinkedHashSet<>();
    public PlayerCardCollection get(UUID playerId) { return players.computeIfAbsent(playerId, ignored -> new PlayerCardCollection()); }
    public void changed() { setDirty(); }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("version", 3); ListTag rewarded = new ListTag();
        processedRewards.forEach(key -> rewarded.add(StringTag.valueOf(key))); tag.put("processed_rewards", rewarded);
        ListTag playerTags = new ListTag();
        players.forEach((playerId, collection) -> {
            CompoundTag player = new CompoundTag(); player.putUUID("player", playerId); player.putInt("gold", collection.gold()); player.putInt("active_deck", collection.activeDeck());
            ListTag cards = new ListTag(); collection.instances().forEach(card -> { CompoundTag value = new CompoundTag(); value.putUUID("id", card.id()); value.putString("card", card.cardId()); value.putInt("star", card.star()); cards.add(value); }); player.put("cards", cards);
            ListTag decks = new ListTag(); collection.decks().forEach(deck -> { CompoundTag value = new CompoundTag(); value.putInt("slot", deck.slot()); value.putString("name", deck.name()); ListTag ids = new ListTag(); deck.cardIds().forEach(id -> { CompoundTag entry = new CompoundTag(); entry.putUUID("id", id); ids.add(entry); }); value.put("cards", ids); decks.add(value); }); player.put("decks", decks);
            playerTags.add(player);
        }); tag.put("players", playerTags); return tag;
    }

    private static PlayerCardSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        PlayerCardSavedData data = new PlayerCardSavedData(); boolean legacy = tag.getInt("version") < 2;
        ListTag rewarded = tag.getList("processed_rewards", Tag.TAG_STRING);
        for (int i = 0; i < rewarded.size(); i++) data.processedRewards.add(rewarded.getString(i));
        ListTag players = tag.getList("players", Tag.TAG_COMPOUND);
        for (int i = 0; i < players.size(); i++) {
            CompoundTag player = players.getCompound(i); PlayerCardCollection collection;
            if (legacy || !player.contains("cards", Tag.TAG_LIST)) collection = loadLegacy(player);
            else {
                collection = new PlayerCardCollection(); ListTag cards = player.getList("cards", Tag.TAG_COMPOUND);
                for (int j = 0; j < cards.size(); j++) { CompoundTag value = cards.getCompound(j); collection.add(new OwnedCardInstance(value.getUUID("id"), value.getString("card"), Math.max(1, value.getInt("star")))); }
                List<PlayerCardCollection.DeckView> decks = new ArrayList<>(); ListTag deckTags = player.getList("decks", Tag.TAG_COMPOUND);
                for (int j = 0; j < deckTags.size(); j++) { CompoundTag value = deckTags.getCompound(j); List<UUID> ids = new ArrayList<>(); ListTag idTags = value.getList("cards", Tag.TAG_COMPOUND); for (int k=0;k<idTags.size();k++) ids.add(idTags.getCompound(k).getUUID("id")); decks.add(new PlayerCardCollection.DeckView(value.getInt("slot"), value.getString("name"), ids)); }
                collection.restoreMeta(player.getInt("gold"), player.getInt("active_deck"), decks);
            }
            data.players.put(player.getUUID("player"), collection);
        }
        if (legacy) data.setDirty(); return data;
    }
    public boolean awardOnce(String key, UUID playerId, String cardId, int gold) {
        return awardOnce(key, playerId, cardId, gold, Set.of());
    }
    public boolean awardOnce(String key, UUID playerId, String cardId, int gold, Collection<String> additionalCards) {
        if (!processedRewards.add(key)) return false;
        PlayerCardCollection collection = get(playerId);
        if (cardId != null && !cardId.isBlank()) collection.grant(cardId, 1);
        additionalCards.forEach(value -> {
            if (value != null && !value.isBlank()) { collection.grant(value, 1); collection.addToDeck(value); }
        });
        setDirty();
        return true;
    }
    private static PlayerCardCollection loadLegacy(CompoundTag player) {
        Map<String,Integer> owned = new LinkedHashMap<>(); ListTag ownedTags = player.getList("owned", Tag.TAG_COMPOUND);
        for(int i=0;i<ownedTags.size();i++){CompoundTag card=ownedTags.getCompound(i);owned.put(card.getString("id"),card.getInt("amount"));}
        List<String> deck = new ArrayList<>(); ListTag deckTags=player.getList("deck",Tag.TAG_STRING);for(int i=0;i<deckTags.size();i++)deck.add(deckTags.getString(i));
        return new PlayerCardCollection(owned, deck);
    }
}
