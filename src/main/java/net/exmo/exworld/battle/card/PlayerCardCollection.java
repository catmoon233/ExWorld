package net.exmo.exworld.battle.card;

import java.nio.charset.StandardCharsets;
import java.util.*;

/** Persistent card inventory: instance ownership, five named presets, fusion and currency behind one interface. */
public final class PlayerCardCollection {
    public static final int MIN_DECK_SIZE = 5, MAX_DECK_SIZE = 30, DECK_SLOTS = 5;
    private final Map<UUID, OwnedCardInstance> cards = new LinkedHashMap<>();
    private final List<DeckPreset> presets = new ArrayList<>();
    private int activeDeck;
    private int gold;

    public PlayerCardCollection() {
        for (int i = 0; i < DECK_SLOTS; i++) presets.add(new DeckPreset(i == 0 ? "Default" : "Deck " + (i + 1), new ArrayList<>()));
    }

    /** Compatibility/migration constructor for the v1 count + one-deck save shape. */
    public PlayerCardCollection(Map<String, Integer> owned, Collection<String> deck) {
        this(); Map<String, ArrayDeque<UUID>> byCard = new LinkedHashMap<>();
        owned.forEach((id, amount) -> {
            for (int i = 0; i < Math.max(0, amount); i++) {
                UUID instanceId = UUID.nameUUIDFromBytes((id + "#" + i).getBytes(StandardCharsets.UTF_8));
                cards.put(instanceId, new OwnedCardInstance(instanceId, id, 1)); byCard.computeIfAbsent(id, ignored -> new ArrayDeque<>()).add(instanceId);
            }
        });
        for (String id : deck) {
            ArrayDeque<UUID> candidates = byCard.get(id); if (candidates != null && !candidates.isEmpty()) presets.getFirst().cardIds.add(candidates.removeFirst());
        }
    }

    public int grant(String cardId, int amount) {
        for (int i = 0; i < Math.max(0, amount); i++) add(OwnedCardInstance.oneStar(cardId));
        return count(cardId);
    }
    public void add(OwnedCardInstance card) { cards.put(card.id(), card); }
    public boolean revoke(String cardId, int amount) {
        List<UUID> removable = cards.values().stream().filter(card -> card.cardId().equals(cardId) && !referenced(card.id())).limit(amount).map(OwnedCardInstance::id).toList();
        if (removable.size() < amount || amount <= 0) return false; removable.forEach(cards::remove); return true;
    }
    public Optional<OwnedCardInstance> fuse(Collection<UUID> materialIds) {
        if (materialIds.size() != 3 || new HashSet<>(materialIds).size() != 3) return Optional.empty();
        List<OwnedCardInstance> materials = materialIds.stream().map(cards::get).filter(Objects::nonNull).toList();
        if (materials.size() != 3 || materials.stream().anyMatch(card -> card.star() >= 5 || referenced(card.id()))) return Optional.empty();
        OwnedCardInstance first = materials.getFirst();
        if (materials.stream().anyMatch(card -> card.star() != first.star() || !card.cardId().equals(first.cardId()))) return Optional.empty();
        materialIds.forEach(cards::remove); OwnedCardInstance fused = new OwnedCardInstance(UUID.randomUUID(), first.cardId(), first.star() + 1); add(fused); return Optional.of(fused);
    }
    public boolean addToDeck(String cardId) {
        Set<UUID> used = new HashSet<>(activePreset().cardIds); OwnedCardInstance candidate = cards.values().stream()
                .filter(card -> card.cardId().equals(cardId) && !used.contains(card.id())).findFirst().orElse(null);
        return candidate != null && addToDeck(activeDeck, candidate.id());
    }
    public boolean addToDeck(int slot, UUID cardId) {
        if (slot < 0 || slot >= DECK_SLOTS || !cards.containsKey(cardId) || presets.get(slot).cardIds.size() >= MAX_DECK_SIZE || presets.get(slot).cardIds.contains(cardId)) return false;
        presets.get(slot).cardIds.add(cardId); return true;
    }
    public boolean removeFromDeck(String cardId) {
        UUID instance = activePreset().cardIds.stream().filter(id -> cards.get(id).cardId().equals(cardId)).findFirst().orElse(null);
        return instance != null && activePreset().cardIds.remove(instance);
    }
    public boolean removeFromDeck(int slot, UUID cardId) { return slot >= 0 && slot < DECK_SLOTS && presets.get(slot).cardIds.remove(cardId); }
    public void clearDeck() { activePreset().cardIds.clear(); }
    public void purgeCards(Set<String> cardIds) {
        presets.forEach(preset -> preset.cardIds.removeIf(id -> {
            OwnedCardInstance card=cards.get(id);return card==null||cardIds.contains(card.cardId());
        }));
        cards.entrySet().removeIf(entry -> cardIds.contains(entry.getValue().cardId()));
    }
    public void purgeCardsMatching(java.util.function.Predicate<String> predicate) {
        Set<String> ids = cards.values().stream().map(OwnedCardInstance::cardId).filter(predicate).collect(java.util.stream.Collectors.toSet());
        purgeCards(ids);
    }
    public boolean replaceDeck(Collection<String> requested) {
        List<UUID> next = new ArrayList<>(); Set<UUID> used = new HashSet<>();
        for (String cardId : requested) {
            UUID found = cards.values().stream().filter(card -> card.cardId().equals(cardId) && !used.contains(card.id())).map(OwnedCardInstance::id).findFirst().orElse(null);
            if (found == null || next.size() >= MAX_DECK_SIZE) return false; used.add(found); next.add(found);
        }
        activePreset().cardIds.clear(); activePreset().cardIds.addAll(next); return true;
    }
    public boolean renameDeck(int slot, String name) { if (slot < 0 || slot >= DECK_SLOTS || name == null || name.isBlank() || name.length() > 32) return false; presets.get(slot).name = name; return true; }
    public boolean setActiveDeck(int slot) { if (slot < 0 || slot >= DECK_SLOTS) return false; activeDeck = slot; return true; }
    public int count(String id) { return (int) cards.values().stream().filter(card -> card.cardId().equals(id)).count(); }
    public int copiesInDeck(String id) { return (int) activePreset().cardIds.stream().map(cards::get).filter(card -> card.cardId().equals(id)).count(); }
    public boolean deckPlayable() { return activePreset().cardIds.size() >= MIN_DECK_SIZE && activePreset().cardIds.size() <= MAX_DECK_SIZE; }
    public Map<String, Integer> owned() { Map<String,Integer> out = new LinkedHashMap<>(); cards.values().forEach(card -> out.merge(card.cardId(), 1, Integer::sum)); return Map.copyOf(out); }
    public List<String> deck() { return activePreset().cardIds.stream().map(cards::get).map(OwnedCardInstance::cardId).toList(); }
    public List<OwnedCardInstance> battleDeckInstances() { return activePreset().cardIds.stream().map(cards::get).toList(); }
    public Collection<OwnedCardInstance> instances() { return List.copyOf(cards.values()); }
    public List<DeckView> decks() { return java.util.stream.IntStream.range(0, presets.size()).mapToObj(i -> new DeckView(i, presets.get(i).name, presets.get(i).cardIds)).toList(); }
    public int activeDeck() { return activeDeck; }
    public int gold() { return gold; }
    public void addGold(int amount) { gold = Math.max(0, gold + amount); }
    /** One-way migration bridge into the shared PlayerResourceVault. */
    public int drainGold() { int value = gold; gold = 0; return value; }
    public void restoreMeta(int gold, int activeDeck, List<DeckView> deckViews) {
        this.gold = Math.max(0, gold); this.activeDeck = Math.max(0, Math.min(DECK_SLOTS - 1, activeDeck));
        for (DeckView view : deckViews) if (view.slot() >= 0 && view.slot() < DECK_SLOTS) { presets.get(view.slot()).name = view.name(); presets.get(view.slot()).cardIds.clear(); view.cardIds().stream().filter(cards::containsKey).forEach(presets.get(view.slot()).cardIds::add); }
    }
    private boolean referenced(UUID id) { return presets.stream().anyMatch(deck -> deck.cardIds.contains(id)); }
    private DeckPreset activePreset() { return presets.get(activeDeck); }
    private static final class DeckPreset { private String name; private final List<UUID> cardIds; private DeckPreset(String name, List<UUID> ids) { this.name=name; cardIds=ids; } }
    public record DeckView(int slot, String name, List<UUID> cardIds) { public DeckView { cardIds = List.copyOf(cardIds); } }
}
