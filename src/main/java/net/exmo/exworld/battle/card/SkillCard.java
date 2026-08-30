package net.exmo.exworld.battle.card;

import java.util.UUID;

public record SkillCard(UUID instanceId, String skillId, int star, boolean innate, boolean exhaust) {
    public SkillCard { star = Math.max(1, Math.min(5, star)); }
    public static SkillCard drawn(String skillId) { return new SkillCard(UUID.randomUUID(), skillId, 1, false, false); }
    public static SkillCard drawn(OwnedCardInstance card) { return new SkillCard(UUID.randomUUID(), card.cardId(), card.star(), false, false); }
    public static SkillCard innate(String skillId) { return new SkillCard(UUID.randomUUID(), skillId, 1, true, false); }
}
