package net.exmo.exworld.battle.card;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Data-driven collectible metadata and five explicit, non-formula star tiers. */
public record CardDefinition(String id, String skillId, String nameKey, String descriptionKey, String icon,
                             String rarity, Set<String> tags, boolean consumable, List<StarTier> stars,
                             CardType type, Set<CardKeyword> keywords, Map<String, Double> effects) {
    public CardDefinition {
        tags = tags == null ? Set.of() : Set.copyOf(tags); stars = List.copyOf(stars);
        type = type == null ? inferType(tags) : type;
        Set<CardKeyword> normalized = new LinkedHashSet<>(keywords == null ? Set.of() : keywords);
        if (consumable || tags.stream().anyMatch(tag -> tag.equalsIgnoreCase("exhaust"))) normalized.add(CardKeyword.EXHAUST);
        if (tags.stream().anyMatch(tag -> tag.equalsIgnoreCase("ethereal"))) normalized.add(CardKeyword.ETHEREAL);
        if (tags.stream().anyMatch(tag -> tag.equalsIgnoreCase("retain") || tag.equalsIgnoreCase("retained"))) normalized.add(CardKeyword.RETAIN);
        keywords = Set.copyOf(normalized);
        effects = effects == null ? Map.of() : Map.copyOf(effects);
        if (stars.size() != 5) throw new IllegalArgumentException("Card " + id + " must define exactly five star tiers");
    }

    /** Compatibility constructor for old datapacks and callers that only supplied free-form tags. */
    public CardDefinition(String id, String skillId, String nameKey, String descriptionKey, String icon,
                          String rarity, Set<String> tags, boolean consumable, List<StarTier> stars) {
        this(id, skillId, nameKey, descriptionKey, icon, rarity, tags, consumable, stars, null, Set.of(), Map.of());
    }

    public CardDefinition(String id, String skillId, String nameKey, String descriptionKey, String icon,
                          String rarity, Set<String> tags, boolean consumable, List<StarTier> stars,
                          CardType type, Set<CardKeyword> keywords) {
        this(id, skillId, nameKey, descriptionKey, icon, rarity, tags, consumable, stars, type, keywords, Map.of());
    }

    public boolean has(CardKeyword keyword) { return keywords.contains(keyword); }
    public double effect(String id) { return effects.getOrDefault(id, 0D); }
    public boolean playable() { return type != CardType.STATUS && type != CardType.CURSE && !tags.contains("unplayable"); }

    private static CardType inferType(Set<String> tags) {
        if (tags.stream().anyMatch(tag -> tag.equalsIgnoreCase("curse") || tag.equalsIgnoreCase("type:curse"))) return CardType.CURSE;
        if (tags.stream().anyMatch(tag -> tag.equalsIgnoreCase("status") || tag.equalsIgnoreCase("type:status"))) return CardType.STATUS;
        if (tags.stream().anyMatch(tag -> tag.equalsIgnoreCase("power") || tag.equalsIgnoreCase("type:power"))) return CardType.POWER;
        if (tags.stream().anyMatch(tag -> tag.equalsIgnoreCase("attack") || tag.equalsIgnoreCase("type:attack"))) return CardType.ATTACK;
        return CardType.SKILL;
    }

    public enum CardType { ATTACK, SKILL, POWER, STATUS, CURSE }
    public enum CardKeyword { ETHEREAL, EXHAUST, RETAIN }

    public StarTier tier(int star) { return stars.get(Math.max(1, Math.min(5, star)) - 1); }
    public record StarTier(int manaCost, int range, Map<String, Double> adapterParameters) {
        public StarTier { manaCost = Math.max(0, manaCost); range = Math.max(0, range); adapterParameters = Map.copyOf(adapterParameters); }
    }
}
