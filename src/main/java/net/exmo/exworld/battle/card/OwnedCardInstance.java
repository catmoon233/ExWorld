package net.exmo.exworld.battle.card;

import java.util.UUID;

/** Permanent collection identity. Battle-local SkillCard ids are always copied from this instance. */
public record OwnedCardInstance(UUID id, String cardId, int star) {
    public OwnedCardInstance {
        if (star < 1 || star > 5) throw new IllegalArgumentException("card star must be 1-5");
    }
    public static OwnedCardInstance oneStar(String cardId) { return new OwnedCardInstance(UUID.randomUUID(), cardId, 1); }
}
