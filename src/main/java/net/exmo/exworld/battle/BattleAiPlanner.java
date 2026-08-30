package net.exmo.exworld.battle;

import net.exmo.exworld.battle.api.BattleSnapshot;
import net.exmo.exworld.battle.api.EncounterRequest;
import net.exmo.exworld.battle.model.BattleCell;
import net.exmo.exworld.battle.model.FactionRelation;
import net.exmo.exworld.battle.skill.SkillDefinition;

import java.util.*;

/** Pure tactical planner. BattleSession remains authoritative for final validation. */
public final class BattleAiPlanner {
    private static final int CELL_EFFECT_RADIUS = 1;
    private static final int MAX_MOVE_CANDIDATES = 24;
    private static final int MAX_CELL_CANDIDATES = 32;

    private BattleAiPlanner() {}

    public enum DecisionType { USE_SKILL, MOVE, FINISH_PHASE }

    public record AiTarget(UUID actorId, UUID targetId, BattleCell targetCell) {
        public AiTarget {
            Objects.requireNonNull(actorId, "actorId");
            Objects.requireNonNull(targetCell, "targetCell");
        }
    }

    public record AiDecision(DecisionType type, UUID actorId, UUID cardInstanceId, AiTarget target) {
        public AiDecision {
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(actorId, "actorId");
            if (type == DecisionType.USE_SKILL) {
                Objects.requireNonNull(cardInstanceId, "cardInstanceId");
                Objects.requireNonNull(target, "target");
            } else if (type == DecisionType.MOVE) {
                Objects.requireNonNull(target, "target");
            }
        }

        public static AiDecision useSkill(UUID actorId, UUID cardInstanceId, AiTarget target) {
            return new AiDecision(DecisionType.USE_SKILL, actorId, cardInstanceId, target);
        }

        public static AiDecision move(UUID actorId, BattleCell destination) {
            return new AiDecision(DecisionType.MOVE, actorId, null, new AiTarget(actorId, null, destination));
        }

        public static AiDecision finish(UUID actorId) {
            return new AiDecision(DecisionType.FINISH_PHASE, actorId, null, null);
        }
    }

    private record ScoredDecision(AiDecision decision, int score, String cardId, String targetKey) {}

    /** Returns all plausible decisions in deterministic tactical priority order. */
    public static List<AiDecision> plan(BattleSession session, BattleSnapshot snapshot,
                                        BattleSnapshot.CombatantView actor) {
        List<ScoredDecision> skills = new ArrayList<>();
        for (BattleSnapshot.CardView card : actor.hand()) {
            if (card.innate() || !card.playable()) continue;
            SkillDefinition definition = session.skillForCard(actor.id(), card.instanceId()).orElse(null);
            if (definition == null) continue;
            for (AiTarget target : targets(session, snapshot, actor, definition, actor.cell())) {
                int score = skillScore(session, snapshot, actor, card, definition, target);
                if (score != Integer.MIN_VALUE) skills.add(new ScoredDecision(
                        AiDecision.useSkill(actor.id(), card.instanceId(), target), score, card.skillId(), targetKey(target)));
            }
        }

        // Basic attack is an explicit fallback after all configured skill cards.
        for (BattleSnapshot.CardView card : actor.hand()) {
            if (!card.innate() || !card.playable()) continue;
            SkillDefinition definition = session.skillForCard(actor.id(), card.instanceId()).orElse(null);
            if (definition == null) continue;
            for (AiTarget target : targets(session, snapshot, actor, definition, actor.cell())) {
                int score = skillScore(session, snapshot, actor, card, definition, target);
                if (score != Integer.MIN_VALUE) skills.add(new ScoredDecision(
                        AiDecision.useSkill(actor.id(), card.instanceId(), target), score - 1_000_000,
                        card.skillId(), targetKey(target)));
            }
        }

        skills.sort(Comparator.comparingInt(ScoredDecision::score).reversed()
                .thenComparing(ScoredDecision::cardId).thenComparing(ScoredDecision::targetKey));
        List<AiDecision> result = new ArrayList<>(skills.size() + MAX_MOVE_CANDIDATES + 1);
        skills.forEach(value -> result.add(value.decision()));
        result.addAll(moveDecisions(session, snapshot, actor));
        result.add(AiDecision.finish(actor.id()));
        return List.copyOf(result);
    }

    private static List<AiTarget> targets(BattleSession session, BattleSnapshot snapshot,
                                          BattleSnapshot.CombatantView actor, SkillDefinition definition,
                                          BattleCell origin) {
        return switch (definition.targetType()) {
            case SELF -> List.of(new AiTarget(actor.id(), actor.id(), origin));
            case ALLY -> snapshot.combatants().values().stream()
                    .filter(candidate -> !candidate.downed() && friendly(session, actor, candidate)
                            && definition.reaches(origin, candidate.cell())
                            && hasLineOfSight(session, actor, candidate, definition, origin, candidate.cell()))
                    .sorted(Comparator.comparingDouble(BattleAiPlanner::healthRatio)
                            .thenComparingInt(candidate -> origin.distanceTo(candidate.cell()))
                            .thenComparing(candidate -> candidate.id().toString()))
                    .map(candidate -> new AiTarget(actor.id(), candidate.id(), candidate.cell())).toList();
            case ENEMY -> {
                boolean tauntForced = snapshot.combatants().values().stream().anyMatch(candidate ->
                        hostile(session, actor, candidate) && candidate.statuses().stream()
                                .anyMatch(status -> status.id().equals("exworld:taunt")));
                yield snapshot.combatants().values().stream()
                    .filter(candidate -> hostile(session, actor, candidate) && definition.reaches(origin, candidate.cell())
                            && hasLineOfSight(session, actor, candidate, definition, origin, candidate.cell())
                            && (!tauntForced || candidate.statuses().stream().anyMatch(status -> status.id().equals("exworld:taunt"))))
                    .sorted(Comparator.comparingInt((BattleSnapshot.CombatantView candidate) ->
                                    -candidate.statuses().stream().filter(status -> status.id().equals("exworld:taunt"))
                                            .mapToInt(BattleSnapshot.StatusView::stacks).findFirst().orElse(0))
                            .thenComparingInt(candidate ->
                                    definition.power() > 0 && definition.power() >= candidate.health() ? 0 : 1)
                            .thenComparingDouble(BattleAiPlanner::healthRatio)
                            .thenComparingInt(candidate -> origin.distanceTo(candidate.cell()))
                            .thenComparing(candidate -> candidate.id().toString()))
                    .map(candidate -> new AiTarget(actor.id(), candidate.id(), candidate.cell())).toList();
            }
            case CELL -> cellTargets(session, snapshot, actor, definition, origin);
        };
    }

    private static List<AiTarget> cellTargets(BattleSession session, BattleSnapshot snapshot,
                                              BattleSnapshot.CombatantView actor, SkillDefinition definition,
                                              BattleCell origin) {
        LinkedHashSet<BattleCell> candidates = new LinkedHashSet<>();
        candidates.add(origin);
        snapshot.combatants().values().stream().filter(candidate -> !candidate.downed())
                .map(BattleSnapshot.CombatantView::cell).forEach(cell -> {
                    candidates.add(cell);
                    for (int dx = -CELL_EFFECT_RADIUS; dx <= CELL_EFFECT_RADIUS; dx++) {
                        for (int dz = -CELL_EFFECT_RADIUS; dz <= CELL_EFFECT_RADIUS; dz++) {
                            if (Math.max(Math.abs(dx), Math.abs(dz)) <= CELL_EFFECT_RADIUS)
                                candidates.add(new BattleCell(cell.x() + dx, cell.z() + dz, cell.floorY()));
                        }
                    }
                });
        return candidates.stream().filter(cell -> inArena(snapshot, cell) && definition.reaches(origin, cell)
                        && (!definition.requiresLineOfSight() || session.hasLineOfSight(origin, cell,
                        actor.id(), null, definition.piercesUnits(), definition.ignoresTerrain())))
                .sorted(Comparator.comparingInt((BattleCell cell) -> cellScore(session, snapshot, actor, definition, cell))
                        .reversed().thenComparing(BattleAiPlanner::cellKey))
                .limit(MAX_CELL_CANDIDATES)
                .map(cell -> new AiTarget(actor.id(), null, cell))
                .filter(target -> cellScore(session, snapshot, actor, definition, target.targetCell()) > 0).toList();
    }

    private static int skillScore(BattleSession session, BattleSnapshot snapshot, BattleSnapshot.CombatantView actor,
                                  BattleSnapshot.CardView card, SkillDefinition definition, AiTarget target) {
        String id = (card.skillId() + " " + definition.id() + " " + definition.nameKey()).toLowerCase(Locale.ROOT);
        boolean healing = definition.aiRole() == SkillDefinition.AiRole.HEAL || containsAny(id, "heal", "first_aid", "recovery", "regeneration", "blessing_of_life");
        boolean support = definition.aiRole() == SkillDefinition.AiRole.BUFF || definition.targetType() == SkillDefinition.TargetType.ALLY
                || containsAny(id, "shield", "fortify", "haste", "oakskin", "protection", "ward", "blessing");
        boolean control = definition.aiRole() == SkillDefinition.AiRole.CONTROL || containsAny(id, "root", "slow", "blight", "poison", "freeze", "stun", "blind", "wither");

        if (healing) {
            if (target.targetId() == null) {
                if (cellScore(session, snapshot, actor, definition, target.targetCell()) <= 0) return Integer.MIN_VALUE;
            } else {
                BattleSnapshot.CombatantView healed = snapshot.combatants().get(target.targetId());
                if (healed == null || healthRatio(healed) >= .999D) return Integer.MIN_VALUE;
            }
        }
        int score = healing ? 900_000 : support ? 800_000 : definition.targetType() == SkillDefinition.TargetType.CELL
                ? 700_000 : control ? 600_000 : 500_000;
        if (target.targetId() != null) {
            BattleSnapshot.CombatantView selected = snapshot.combatants().get(target.targetId());
            if (selected != null) {
                if (!support && definition.power() > 0 && definition.power() >= selected.health()) score += 50_000;
                score += (int) ((1.0D - healthRatio(selected)) * 10_000);
                score -= actor.cell().distanceTo(selected.cell()) * 10;
                if (healing) score += (int) ((1.0D - healthRatio(selected)) * 40_000);
            }
        } else {
            score += cellScore(session, snapshot, actor, definition, target.targetCell()) * 500;
        }
        return score;
    }

    private static List<AiDecision> moveDecisions(BattleSession session, BattleSnapshot snapshot,
                                                   BattleSnapshot.CombatantView actor) {
        int budget = actor.movementRemaining();
        if (budget <= 0) return List.of();
        int currentPotential = positionScore(session, snapshot, actor, actor.cell());
        int preferredDistance = preferredCombatDistance(session, snapshot, actor);
        Set<String> occupied = snapshot.combatants().values().stream()
                .filter(candidate -> !candidate.downed() && !candidate.id().equals(actor.id()))
                .map(candidate -> candidate.cell().x() + ":" + candidate.cell().z())
                .collect(java.util.stream.Collectors.toSet());
        List<ScoredDecision> moves = new ArrayList<>();
        for (int dx = -budget; dx <= budget; dx++) for (int dz = -budget; dz <= budget; dz++) {
            if (Math.max(Math.abs(dx), Math.abs(dz)) > budget) continue;
            BattleCell destination = new BattleCell(actor.cell().x() + dx, actor.cell().z() + dz, actor.cell().floorY());
            if (destination.equals(actor.cell()) || destination.x() < 0 || destination.z() < 0
                    || destination.x() >= snapshot.arenaSize() || destination.z() >= snapshot.arenaSize()
                    || occupied.contains(destination.x() + ":" + destination.z())) continue;
            int potential = positionScore(session, snapshot, actor, destination);
            int distance = nearestHostileDistance(session, snapshot, actor, destination);
            int currentSafety = Math.abs(nearestHostileDistance(session, snapshot, actor, actor.cell()) - preferredDistance);
            int destinationSafety = Math.abs(distance - preferredDistance);
            if (potential <= currentPotential && destinationSafety >= currentSafety) continue;
            int safety = distance == 999 ? 0 : Math.abs(distance - preferredDistance);
            int score = potential * 10_000 - safety * 500 - destination.distanceTo(actor.cell());
            moves.add(new ScoredDecision(AiDecision.move(actor.id(), destination), score, "", cellKey(destination)));
        }
        moves.sort(Comparator.comparingInt(ScoredDecision::score).reversed().thenComparing(ScoredDecision::targetKey));
        return moves.stream().limit(MAX_MOVE_CANDIDATES).map(ScoredDecision::decision).toList();
    }

    private static int positionScore(BattleSession session, BattleSnapshot snapshot,
                                     BattleSnapshot.CombatantView actor, BattleCell position) {
        BattleSnapshot.CombatantView positioned = withCell(actor, position);
        int score = 0;
        for (BattleSnapshot.CardView card : actor.hand()) {
            if (!card.playable()) continue;
            SkillDefinition definition = session.skillForCard(actor.id(), card.instanceId()).orElse(null);
            if (definition == null) continue;
            int usableTargets = (int) targets(session, snapshot, positioned, definition, position).stream()
                    .filter(target -> skillScore(session, snapshot, positioned, card, definition, target) != Integer.MIN_VALUE)
                    .count();
            score += (card.innate() ? 1 : 10) * Math.min(8, usableTargets);
        }
        return score;
    }

    private static int preferredCombatDistance(BattleSession session, BattleSnapshot snapshot,
                                               BattleSnapshot.CombatantView actor) {
        int preferred = 1;
        boolean hasEnemyOrCellSkill = false;
        for (BattleSnapshot.CardView card : actor.hand()) {
            if (!card.playable()) continue;
            SkillDefinition definition = session.skillForCard(actor.id(), card.instanceId()).orElse(null);
            if (definition == null) continue;
            if (definition.targetType() == SkillDefinition.TargetType.ENEMY
                    || definition.targetType() == SkillDefinition.TargetType.CELL) {
                hasEnemyOrCellSkill = true;
                preferred = Math.max(preferred, definition.range());
            }
        }
        return hasEnemyOrCellSkill ? preferred : 1;
    }

    private static int cellScore(BattleSession session, BattleSnapshot snapshot,
                                 BattleSnapshot.CombatantView actor, SkillDefinition definition, BattleCell cell) {
        String id = (definition.id() + " " + definition.nameKey()).toLowerCase(Locale.ROOT);
        boolean healing = containsAny(id, "heal", "first_aid", "recovery", "regeneration", "blessing_of_life");
        boolean support = containsAny(id, "shield", "fortify", "haste", "oakskin", "blessing", "ward", "protection");
        return (int) snapshot.combatants().values().stream().filter(candidate -> !candidate.downed())
                .filter(candidate -> cell.distanceTo(candidate.cell()) <= CELL_EFFECT_RADIUS)
                .filter(candidate -> healing
                        ? friendly(session, actor, candidate) && healthRatio(candidate) < .999D
                        : support ? friendly(session, actor, candidate) : hostile(session, actor, candidate)).count();
    }

    private static boolean hasLineOfSight(BattleSession session, BattleSnapshot.CombatantView actor,
                                          BattleSnapshot.CombatantView target, SkillDefinition definition,
                                          BattleCell origin, BattleCell targetCell) {
        return !definition.requiresLineOfSight() || session.hasLineOfSight(origin, targetCell,
                actor.id(), target == null ? null : target.id(), definition.piercesUnits(), definition.ignoresTerrain());
    }

    private static boolean inArena(BattleSnapshot snapshot, BattleCell cell) {
        return cell.x() >= 0 && cell.z() >= 0 && cell.x() < snapshot.arenaSize() && cell.z() < snapshot.arenaSize();
    }

    private static boolean friendly(BattleSession session, BattleSnapshot.CombatantView actor,
                                    BattleSnapshot.CombatantView candidate) {
        return !candidate.downed()
                && session.request().relations().getOrDefault(
                new EncounterRequest.FactionPair(actor.factionId(), candidate.factionId()),
                actor.factionId().equals(candidate.factionId()) ? FactionRelation.FRIENDLY : FactionRelation.HOSTILE)
                == FactionRelation.FRIENDLY;
    }

    private static boolean hostile(BattleSession session, BattleSnapshot.CombatantView actor,
                                   BattleSnapshot.CombatantView candidate) {
        return !candidate.id().equals(actor.id()) && !candidate.downed()
                && session.request().relations().getOrDefault(
                new EncounterRequest.FactionPair(actor.factionId(), candidate.factionId()), FactionRelation.HOSTILE)
                == FactionRelation.HOSTILE;
    }

    private static int nearestHostileDistance(BattleSession session, BattleSnapshot snapshot,
                                              BattleSnapshot.CombatantView actor, BattleCell origin) {
        return snapshot.combatants().values().stream().filter(candidate -> hostile(session, actor, candidate))
                .mapToInt(candidate -> origin.distanceTo(candidate.cell())).min().orElse(999);
    }

    private static BattleSnapshot.CombatantView withCell(BattleSnapshot.CombatantView actor, BattleCell cell) {
        return new BattleSnapshot.CombatantView(actor.id(), actor.playerId(), actor.name(), actor.factionId(), cell, actor.attributes(),
                actor.health(), actor.maxHealth(), actor.block(), actor.mana(), actor.maxMana(), actor.movementRemaining(), actor.actionPoints(), actor.actionPointsRemaining(),
                actor.downed(), actor.autoBattle(), actor.hand(), actor.drawPile(), actor.discardPile(), actor.statuses(), actor.itemUseLimit(),
                actor.itemUsesRemaining(), actor.activeWeaponSlot(), actor.weaponSlots(), actor.items());
    }

    private static double healthRatio(BattleSnapshot.CombatantView value) {
        return value.maxHealth() <= 0 ? 1.0D : value.health() / value.maxHealth();
    }

    private static boolean containsAny(String value, String... terms) {
        for (String term : terms) if (value.contains(term)) return true;
        return false;
    }

    private static String targetKey(AiTarget target) {
        return target.targetId() == null ? cellKey(target.targetCell()) : target.targetId().toString();
    }

    private static String cellKey(BattleCell cell) { return cell.x() + ":" + cell.z() + ":" + cell.floorY(); }
}
