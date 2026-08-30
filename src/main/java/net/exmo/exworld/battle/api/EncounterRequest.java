package net.exmo.exworld.battle.api;

import net.exmo.exworld.battle.model.BattleCell;
import net.exmo.exworld.battle.model.CombatantAttribute;
import net.exmo.exworld.battle.model.FactionRelation;
import net.exmo.exworld.battle.card.DeckState;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Stable seam used by maps and scripts to request combat without knowing BattleSession internals. */
public record EncounterRequest(
        String encounterId,
        String arenaId,
        List<CombatantSeed> combatants,
        Map<FactionPair, FactionRelation> relations,
        Map<UUID, ReturnPoint> returnPoints,
        long seed,
        Map<String, String> variables,
        String openingFaction,
        String rewardPool,
        String introProfile,
        BattleHost host
) {
    public EncounterRequest {
        combatants = List.copyOf(combatants);
        relations = Map.copyOf(relations);
        returnPoints = Map.copyOf(returnPoints);
        variables = Map.copyOf(variables);
        host = host == null ? BattleHost.isolated() : host;
    }

    /** Compatibility constructor for map/script callers that use initiative for the opening phase. */
    public EncounterRequest(String encounterId, String arenaId, List<CombatantSeed> combatants,
                            Map<FactionPair, FactionRelation> relations, Map<UUID, ReturnPoint> returnPoints,
                            long seed, Map<String, String> variables) {
        this(encounterId, arenaId, combatants, relations, returnPoints, seed, variables, null);
    }
    /** Compatibility constructor for callers that specify only the opening faction. */
    public EncounterRequest(String encounterId, String arenaId, List<CombatantSeed> combatants,
                            Map<FactionPair, FactionRelation> relations, Map<UUID, ReturnPoint> returnPoints,
                            long seed, Map<String, String> variables, String openingFaction) {
        this(encounterId, arenaId, combatants, relations, returnPoints, seed, variables, openingFaction, null, null);
    }

    public EncounterRequest(String encounterId, String arenaId, List<CombatantSeed> combatants,
                            Map<FactionPair, FactionRelation> relations, Map<UUID, ReturnPoint> returnPoints,
                            long seed, Map<String, String> variables, String openingFaction,
                            String rewardPool, String introProfile) {
        this(encounterId, arenaId, combatants, relations, returnPoints, seed, variables, openingFaction,
                rewardPool, introProfile, BattleHost.isolated());
    }

    public record CombatantSeed(UUID entityId, UUID playerId, String name, String factionId, BattleCell cell,
                                float maxHealth, float health, float maxMana, float mana,
                                float manaPerPhase, double initiative, int movementPoints, int initialHandSize, List<String> deck,
                                List<Integer> deckStars, int actionPoints, int drawPerPhase, java.util.Set<CombatantAttribute> attributes) {
        public CombatantSeed { deck = List.copyOf(deck); deckStars = List.copyOf(deckStars); attributes = attributes == null ? java.util.Set.of() : java.util.Set.copyOf(attributes); actionPoints = Math.max(-1, actionPoints); drawPerPhase = Math.max(0, drawPerPhase); if (deckStars.size() != deck.size()) throw new IllegalArgumentException("deck star count mismatch"); }
        /** Compatibility constructor for ordinary ground combatants. */
        public CombatantSeed(UUID entityId, UUID playerId, String name, String factionId, BattleCell cell,
                             float maxHealth, float health, float maxMana, float mana, float manaPerPhase,
                             double initiative, int movementPoints, int initialHandSize, List<String> deck,
                             List<Integer> deckStars, int actionPoints, int drawPerPhase) {
            this(entityId, playerId, name, factionId, cell, maxHealth, health, maxMana, mana, manaPerPhase,
                    initiative, movementPoints, initialHandSize, deck, deckStars, actionPoints, drawPerPhase, java.util.Set.of());
        }
        public CombatantSeed(UUID entityId, UUID playerId, String name, String factionId, BattleCell cell,
                             float maxHealth, float health, float maxMana, float mana, float manaPerPhase,
                             double initiative, int movementPoints, int initialHandSize, List<String> deck,
                             List<Integer> deckStars, int actionPoints) {
            this(entityId, playerId, name, factionId, cell, maxHealth, health, maxMana, mana, manaPerPhase,
                    initiative, movementPoints, initialHandSize, deck, deckStars, actionPoints, DeckState.DRAW_PER_PHASE);
        }
        public CombatantSeed(UUID entityId, UUID playerId, String name, String factionId, BattleCell cell,
                             float maxHealth, float health, float maxMana, float mana, float manaPerPhase,
                             double initiative, int movementPoints, int initialHandSize, List<String> deck,
                             List<Integer> deckStars) {
            this(entityId, playerId, name, factionId, cell, maxHealth, health, maxMana, mana, manaPerPhase,
                    initiative, movementPoints, initialHandSize, deck, deckStars, -1, DeckState.DRAW_PER_PHASE);
        }
        public CombatantSeed(UUID entityId, UUID playerId, String name, String factionId, BattleCell cell,
                             float maxHealth, float health, float maxMana, float mana, float manaPerPhase,
                             double initiative, int movementPoints, int initialHandSize, List<String> deck) {
            this(entityId, playerId, name, factionId, cell, maxHealth, health, maxMana, mana, manaPerPhase,
                    initiative, movementPoints, initialHandSize, deck, java.util.Collections.nCopies(deck.size(), 1), -1, DeckState.DRAW_PER_PHASE);
        }
        public CombatantSeed(UUID entityId, UUID playerId, String name, String factionId, BattleCell cell,
                             float maxHealth, float health, float maxMana, float mana, float manaPerPhase,
                             double initiative, int movementPoints, int initialHandSize, List<String> deck, int actionPoints) {
            this(entityId, playerId, name, factionId, cell, maxHealth, health, maxMana, mana, manaPerPhase,
                    initiative, movementPoints, initialHandSize, deck, java.util.Collections.nCopies(deck.size(), 1), actionPoints, DeckState.DRAW_PER_PHASE);
        }
        public CombatantSeed(UUID entityId, UUID playerId, String name, String factionId, BattleCell cell,
                             float maxHealth, float health, float maxMana, float mana, float manaPerPhase,
                             double initiative, int movementPoints, List<String> deck) {
            this(entityId, playerId, name, factionId, cell, maxHealth, health, maxMana, mana, manaPerPhase,
                    initiative, movementPoints, DeckState.DEFAULT_INITIAL_HAND, deck, java.util.Collections.nCopies(deck.size(), 1), -1, DeckState.DRAW_PER_PHASE);
        }
        public CombatantSeed(UUID entityId, UUID playerId, String name, String factionId, BattleCell cell,
                             float maxHealth, float health, float maxMana, float mana, float manaPerPhase,
                             double initiative, int movementPoints, List<String> deck, int actionPoints) {
            this(entityId, playerId, name, factionId, cell, maxHealth, health, maxMana, mana, manaPerPhase,
                    initiative, movementPoints, DeckState.DEFAULT_INITIAL_HAND, deck, java.util.Collections.nCopies(deck.size(), 1), actionPoints, DeckState.DRAW_PER_PHASE);
        }
        public boolean playerControlled() { return playerId != null; }
    }

    public record FactionPair(String from, String to) {}
    public record ReturnPoint(String dimension, double x, double y, double z, float yaw, float pitch) {}
}
