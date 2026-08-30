package net.exmo.exworld.battle.api;

import net.exmo.exworld.battle.model.BattleCell;
import net.exmo.exworld.battle.model.CombatantAttribute;
import net.exmo.exworld.battle.model.BattleState;
import net.exmo.exworld.battle.skill.SkillDefinition;
import net.exmo.exworld.battle.card.CardDefinition;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Collections;
import java.util.LinkedHashMap;

/** Immutable read model shared by callers, network serialization and AI. */
public record BattleSnapshot(BattleId battleId, long revision, long eventSequence, BattleState state,
                             String arenaId, int arenaSize, int arenaOriginX, int arenaOriginZ,
                             List<BattleCell> blockedCells,
                             String biomeId,
                             int round, String activeFaction, List<String> factionOrder, int phaseTicksRemaining,
                             Map<UUID, CombatantView> combatants, Set<UUID> readyPlayers, List<BattleEvent> events,
                             List<MotionView> motions, IntroView intro, ResultView result, double phaseDamage) {
    public BattleSnapshot {
        blockedCells = List.copyOf(blockedCells);
        factionOrder = List.copyOf(factionOrder);
        combatants = Collections.unmodifiableMap(new LinkedHashMap<>(combatants));
        readyPlayers = Set.copyOf(readyPlayers);
        events = List.copyOf(events);
        motions = List.copyOf(motions);
    }

    /** Returns the same authoritative snapshot with private inventory projections scoped to one player. */
    public BattleSnapshot forViewer(UUID viewerId) {
        Map<UUID, CombatantView> filtered = new LinkedHashMap<>();
        combatants.forEach((id, view) -> {
            boolean owner = viewerId != null && viewerId.equals(view.playerId());
            filtered.put(id, new CombatantView(view.id(), view.playerId(), view.name(), view.factionId(), view.cell(), view.attributes(),
                    view.health(), view.maxHealth(), view.block(), view.mana(), view.maxMana(), view.movementRemaining(), view.actionPoints(), view.actionPointsRemaining(), view.downed(), view.autoBattle(),
                    view.hand(), view.drawPile(), view.discardPile(), view.statuses(), view.itemUseLimit(), view.itemUsesRemaining(),
                    view.activeWeaponSlot(), owner ? view.weaponSlots() : List.of(), owner ? view.items() : List.of()));
        });
        return new BattleSnapshot(battleId, revision, eventSequence, state, arenaId, arenaSize, arenaOriginX, arenaOriginZ, blockedCells, biomeId,
                round, activeFaction, factionOrder, phaseTicksRemaining, filtered, readyPlayers, events, motions, intro, result, phaseDamage);
    }

    public record CombatantView(UUID id, UUID playerId, String name, String factionId, BattleCell cell, Set<CombatantAttribute> attributes,
                                float health, float maxHealth, float block, float mana, float maxMana,
                                int movementRemaining, int actionPoints, int actionPointsRemaining, boolean downed, boolean autoBattle,
                                List<CardView> hand, List<CardView> drawPile, List<CardView> discardPile, List<StatusView> statuses,
                                int itemUseLimit, int itemUsesRemaining, int activeWeaponSlot,
                                List<WeaponSlotView> weaponSlots, List<ItemView> items) {
        public CombatantView { attributes = Set.copyOf(attributes); hand = List.copyOf(hand); drawPile=List.copyOf(drawPile);discardPile=List.copyOf(discardPile); statuses = List.copyOf(statuses); }
        public CombatantView(UUID id, UUID playerId, String name, String factionId, BattleCell cell,
                             float health, float maxHealth, float mana, float maxMana,
                             int movementRemaining, boolean downed, boolean autoBattle,
                             List<CardView> hand, List<CardView> drawPile, List<CardView> discardPile, List<StatusView> statuses) {
            this(id, playerId, name, factionId, cell, Set.of(), health, maxHealth, 0.0F, mana, maxMana, movementRemaining, 0, 0, downed, autoBattle,
                    hand, drawPile, discardPile, statuses, 1, 1, 0, List.of(), List.of());
        }
        public int drawCount(){return drawPile.size();}
        public int discardCount(){return discardPile.size();}
    }
    public record ItemView(int inventorySlot, String itemId, String displayName, int count, boolean usable, String targetType) {}
    public record WeaponSlotView(int slot, String itemId, String displayName, boolean available, String passiveId, int passiveProgress) {}
    public record StatusView(String id, String nameKey, int stacks, int remainingRounds, boolean beneficial) {}
    public record CardView(UUID instanceId, String skillId, String nameKey, String descriptionKey, String icon, boolean innate,
                           int star, int manaCost, int range, SkillDefinition.TargetType targetType, boolean playable, boolean retained,
                           CardDefinition.CardType type, Set<CardDefinition.CardKeyword> keywords, boolean chebyshevRange) {
        public CardView(UUID instanceId, String skillId, String nameKey, String descriptionKey, String icon, boolean innate,
                        int star, int manaCost, int range, SkillDefinition.TargetType targetType, boolean playable, boolean retained) {
            this(instanceId, skillId, nameKey, descriptionKey, icon, innate, star, manaCost, range, targetType, playable, retained,
                    CardDefinition.CardType.SKILL, Set.of(), false);
        }
        public CardView { keywords = Set.copyOf(keywords); }
        public boolean reaches(BattleCell origin, BattleCell cell) {
            if (origin == null || cell == null) return false;
            return chebyshevRange ? origin.distanceTo(cell) <= range : origin.withinRadius(cell, range);
        }
    }
    public record MotionView(UUID actorId, BattleCell start, List<BattleCell> path, int durationTicks, int elapsedTicks) {
        public MotionView { path = List.copyOf(path); }
    }
    public record IntroView(int elapsedTicks, int totalTicks, Set<UUID> skippedPlayers, int requiredPlayers) {
        public IntroView { skippedPlayers = Set.copyOf(skippedPlayers); }
    }
    public record ResultView(String outcome, int ticksRemaining, int rounds, long durationTicks, int downedUnits,
                             Map<UUID, PlayerResultView> players) {
        public ResultView { players = Map.copyOf(players); }
    }
    public record PlayerResultView(List<String> candidates, String selectedCandidate, boolean confirmed,
                                   int gold, double damageDealt, double damageTaken, double healing, int cardsUsed) {
        public PlayerResultView { candidates = List.copyOf(candidates); }
    }
}
