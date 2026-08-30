package net.exmo.exworld.battle.combat;

import net.exmo.exworld.battle.BattleSession;
import net.exmo.exworld.battle.api.event.BattleEvents;
import net.exmo.exworld.battle.arena.ArenaGrid;
import net.exmo.exworld.battle.combatant.Combatant;
import net.exmo.exworld.battle.model.BattleCell;
import net.exmo.exworld.battle.model.BattleCells;
import net.exmo.exworld.battle.model.BattleStatus;
import net.exmo.exworld.battle.model.FactionRelation;
import net.exmo.exworld.battle.skill.BattleSkillIds;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Dodge, guard and intercept reactions. BattleSession remains authoritative for occupancy and damage;
 * this module only decides how incoming hits and paths change.
 */
public final class CombatReactionRules {
    private static boolean registered;
    private static final Map<UUID, Wave> waves = new HashMap<>();

    private CombatReactionRules() {}

    public static synchronized void ensureRegistered() {
        if (registered) return;
        registered = true;
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, CombatReactionRules::onDamage);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, CombatReactionRules::onStatus);
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, CombatReactionRules::onWeaponSwitched);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, CombatReactionRules::onSkillResolved);
    }

    public static void beginWave(BattleSession session) {
        waves.put(session.id().value(), new Wave());
    }

    public static void clearWave(BattleSession session) {
        waves.remove(session.id().value());
    }

    public record Intercept(Combatant interceptor, BattleCell intersection, List<BattleCell> truncatedPath, double damage) {
        public Intercept { truncatedPath = List.copyOf(truncatedPath); }
    }

    public static Optional<Intercept> interceptOrdinaryMove(BattleSession session, Combatant mover, BattleCell start,
                                                            List<BattleCell> path) {
        if (mover == null || path == null || path.isEmpty()) return Optional.empty();
        Set<String> pathKeys = new HashSet<>();
        pathKeys.add(key(start));
        path.forEach(cell -> pathKeys.add(key(cell)));
        for (Combatant interceptor : session.combatants()) {
            if (interceptor.downed() || interceptor.id().equals(mover.id())) continue;
            if (session.relationOf(interceptor.factionId(), mover.factionId()) != FactionRelation.HOSTILE) continue;
            BattleStatus intercept = interceptor.status(BattleSkillIds.INTERCEPT_STATUS).orElse(null);
            if (intercept == null || interceptor.interceptDx() == 0 && interceptor.interceptDz() == 0) continue;
            List<BattleCell> ray = BattleCells.ray(interceptor.cell(), interceptor.interceptDx(), interceptor.interceptDz(),
                    intercept.stacks());
            BattleCell intersection = null;
            for (BattleCell cell : ray) {
                if (pathKeys.contains(key(cell))) { intersection = cell; break; }
            }
            if (intersection == null) continue;
            List<BattleCell> full = new ArrayList<>();
            full.add(start);
            full.addAll(path);
            int hitIndex = indexOf(full, intersection);
            if (hitIndex < 0) continue;
            BattleCell stop = hitIndex <= 0 ? start : full.get(hitIndex - 1);
            List<BattleCell> truncated = new ArrayList<>();
            for (int i = 1; i < full.size(); i++) {
                BattleCell cell = full.get(i);
                truncated.add(cell);
                if (cell.equals(stop)) break;
            }
            if (stop.equals(start)) truncated = List.of();
            double damage = mover.weaponAttack() * (1.0D + intercept.stacks() * 0.05D);
            return Optional.of(new Intercept(interceptor, intersection, truncated, damage));
        }
        return Optional.empty();
    }

    /** Stops a skill displacement on the cell in front of a guarding hostile on the Chebyshev line. */
    public static BattleCell haltSkillDestination(BattleSession session, Combatant mover, BattleCell start,
                                                  BattleCell destination) {
        if (mover == null || destination == null || start.equals(destination)) return destination;
        List<BattleCell> line = BattleCells.chebyshevLine(start, destination);
        for (int i = 0; i < line.size(); i++) {
            BattleCell cell = line.get(i);
            Combatant occupant = session.combatants().stream()
                    .filter(candidate -> !candidate.downed() && !candidate.id().equals(mover.id())
                            && candidate.cell().x() == cell.x() && candidate.cell().z() == cell.z())
                    .findFirst().orElse(null);
            if (occupant == null || occupant.status(BattleSkillIds.GUARDING).isEmpty()) continue;
            if (session.relationOf(occupant.factionId(), mover.factionId()) != FactionRelation.HOSTILE) continue;
            return i == 0 ? start : line.get(i - 1);
        }
        return destination;
    }

    public static boolean displaceFree(BattleSession session, Combatant actor, BattleCell destination) {
        if (actor == null || destination == null || actor.cell().equals(destination)) return false;
        ArenaGrid arena = session.arena();
        if (arena.occupant(destination).isPresent()) return false;
        arena.remove(actor.id());
        if (!arena.place(actor.id(), destination)) {
            arena.place(actor.id(), actor.cell());
            return false;
        }
        actor.relocate(destination);
        return true;
    }

    public static void onDamage(BattleEvents.DamageAboutToBeDealt event) {
        if (event.reactionsApplied()) return;
        event.markReactionsApplied();
        Combatant source = event.source(), target = event.target();
        if (source == null || target == null || target.downed()) return;
        Wave wave = waves.computeIfAbsent(event.session().id().value(), ignored -> new Wave());
        if (shouldDodge(event.session(), source, target)) {
            Boolean immune = resolveDodge(event.session(), wave, target, event.affectedCells());
            if (Boolean.TRUE.equals(immune)) {
                event.cancel("battle.command.dodged");
                return;
            }
        }
        if (event.cancelled() || event.amount() <= 0) return;
        BattleStatus aura = source.status(BattleSkillIds.BATTLE_AURA_STATUS).orElse(null);
        if (aura != null && source.mana() >= 1) {
            source.spendMana(1);
            event.amount(event.amount() + source.weaponAttack() * 0.20D);
            if (source.mana() <= 0) source.removeStatus(BattleSkillIds.BATTLE_AURA_STATUS);
        }
        if (event.damageType() == BattleDamageType.PHYSICAL) {
            if (source.status(BattleSkillIds.EXHAUSTION).isPresent()) event.amount(event.amount() * 0.25D);
            target.status(BattleSkillIds.GUARDING).ifPresent(guard ->
                    event.amount(event.amount() * Math.max(0, 1.0D - guard.stacks() * 0.10D)));
        }
    }

    public static void onStatus(BattleEvents.StatusAboutToBeApplied event) {
        if (event.reactionsApplied()) return;
        event.markReactionsApplied();
        Combatant source = event.source(), target = event.target();
        if (source == null || target == null) return;
        Wave wave = waves.computeIfAbsent(event.session().id().value(), ignored -> new Wave());
        if (wave.immune.contains(target.id())) {
            event.cancel("battle.command.dodged");
            return;
        }
        if (shouldDodge(event.session(), source, target)) {
            Boolean immune = resolveDodge(event.session(), wave, target, Set.of(target.cell()));
            if (Boolean.TRUE.equals(immune)) event.cancel("battle.command.dodged");
        }
    }

    private static void onSkillResolved(BattleEvents.SkillResolved event) {
        clearWave(event.session());
    }

    public static void onWeaponSwitched(BattleEvents.WeaponSwitched event) {
        event.actor().removeStatus(BattleSkillIds.BATTLE_AURA_STATUS);
    }

    private static boolean shouldDodge(BattleSession session, Combatant source, Combatant target) {
        if (source.id().equals(target.id())) return false;
        if (target.status(BattleSkillIds.DODGE_STATUS).isEmpty()) return false;
        return session.relationOf(source.factionId(), target.factionId()) == FactionRelation.HOSTILE
                && source.factionId().equals(session.activeFaction());
    }

    private static Boolean resolveDodge(BattleSession session, Wave wave, Combatant target, Set<BattleCell> affected) {
        if (wave.resolved.contains(target.id())) return wave.immune.contains(target.id());
        wave.resolved.add(target.id());
        BattleStatus dodge = target.status(BattleSkillIds.DODGE_STATUS).orElse(null);
        if (dodge == null || (target.dodgeDx() == 0 && target.dodgeDz() == 0)) return false;
        BattleCell origin = target.cell();
        BattleCell destination = origin;
        for (int step = 1; step <= dodge.stacks(); step++) {
            BattleCell next = BattleCells.offset(origin, target.dodgeDx() * step, target.dodgeDz() * step);
            if (!displaceFree(session, target, next)) break;
            destination = next;
        }
        Set<BattleCell> hit = affected == null || affected.isEmpty() ? Set.of(origin) : affected;
        boolean immune = !hit.contains(destination);
        if (immune) wave.immune.add(target.id());
        return immune;
    }

    private static int indexOf(List<BattleCell> cells, BattleCell target) {
        for (int i = 0; i < cells.size(); i++) if (cells.get(i).x() == target.x() && cells.get(i).z() == target.z()) return i;
        return -1;
    }

    private static String key(BattleCell cell) { return cell.x() + ":" + cell.z(); }

    private static final class Wave {
        private final Set<UUID> resolved = new HashSet<>();
        private final Set<UUID> immune = new HashSet<>();
    }
}