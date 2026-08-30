package net.exmo.exworld.battle.card;

import java.util.List;

public record CardCollectionSnapshot(int gold, int activeDeck, List<OwnedCardInstance> cards,
                                     List<PlayerCardCollection.DeckView> decks, List<CardSummary> definitions) {
    public CardCollectionSnapshot { cards = List.copyOf(cards); decks = List.copyOf(decks); definitions = List.copyOf(definitions); }
    public record CardSummary(String id, String nameKey, String descriptionKey, String rarity, List<String> tags,
                              List<CardDefinition.StarTier> stars) {
        public CardSummary { tags = List.copyOf(tags); stars = List.copyOf(stars); }
    }
}
