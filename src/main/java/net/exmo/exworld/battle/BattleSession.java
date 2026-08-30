package net.exmo.exworld.battle;

import net.exmo.exworld.battle.api.*;
import net.exmo.exworld.battle.action.BattleActionTimeline;
import net.exmo.exworld.battle.arena.ArenaGrid;
import net.exmo.exworld.battle.card.SkillCard;
import net.exmo.exworld.battle.combatant.Combatant;
import net.exmo.exworld.battle.combat.BattleDamageType;
import net.exmo.exworld.battle.combat.BattleEffectResolver;
import net.exmo.exworld.battle.combat.CombatReactionRules;
import net.exmo.exworld.battle.model.BattleCell;
import net.exmo.exworld.battle.model.BattleState;
import net.exmo.exworld.battle.model.FactionRelation;
import net.exmo.exworld.battle.model.BattleStatus;
import net.exmo.exworld.battle.skill.*;
import net.exmo.exworld.battle.data.IntroProfile;
import net.exmo.exworld.battle.item.BattleItemAccess;
import net.exmo.exworld.battle.weapon.WeaponPassiveEngine;
import net.exmo.exworld.battle.api.event.BattleEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/** Deep aggregate: callers submit intent while phase, deck, grid and validation rules remain local. */
public final class BattleSession {
    /** Time after everyone ends their action during which readiness can still be withdrawn. */
    public static final int READY_GRACE_TICKS = 20;
    public static final int DEFAULT_DEPLOYMENT_TICKS = 20 * 20;
    public static final int DEFAULT_PHASE_TICKS = 45 * 20;
    public static final int DEFAULT_INTRO_TICKS = 8 + 35 * 2 + 25;
    public static final int OUTCOME_PRESENTATION_TICKS = 30;
    public static final int RESULT_TIMEOUT_TICKS = 60 * 20;
    public static final int RETURNING_TICKS = 10;
    private final BattleId id;
    private final EncounterRequest request;
    private final ArenaGrid arena;
    private final int arenaOriginX;
    private final int arenaOriginZ;
    private final SkillRegistry skills;
    private final BattleEffectResolver effects;
    private BattleItemAccess itemAccess = BattleItemAccess.unavailable();
    private final WeaponPassiveEngine passiveEngine = new WeaponPassiveEngine();
    private final Map<UUID, Combatant> combatants = new LinkedHashMap<>();
    private final Map<UUID, CommandReceipt> receipts = new LinkedHashMap<>();
    private final Set<UUID> readyPlayers = new LinkedHashSet<>();
    private int readyTicks;
    private final Set<UUID> introSkippedPlayers = new LinkedHashSet<>();
    private final ArrayDeque<BattleEvent> events = new ArrayDeque<>();
    private final BattleActionTimeline actions = new BattleActionTimeline();
    private final Map<UUID, Integer> actionLocks = new LinkedHashMap<>();
    private final Map<UUID, PendingDash> pendingDashes = new LinkedHashMap<>();
    private final Map<UUID, MutableStatistics> statistics = new LinkedHashMap<>();
    private final Map<UUID, RewardState> rewards = new LinkedHashMap<>();
    private final List<String> factionOrder;
    private final int introTotalTicks;
    private final IntroProfile introProfile;
    private BattleState state = BattleState.CREATING;
    private long revision;
    private long eventSequence;
    private int round;
    private int factionIndex;
    private int phaseTicksRemaining;
    private long battleTicks;
    private double phaseDamage;
    private BattleResult pendingResult;
    private boolean resultPublished;
    private Consumer<BattleResult> resultConsumer = ignored -> {};
    private Consumer<BattleEvent> eventConsumer = ignored -> {};
    /** The authoritative battle event transport is NeoForge's global event bus. */
    private final IEventBus eventBus = NeoForge.EVENT_BUS;

    public BattleSession(BattleId id, EncounterRequest request, ArenaGrid arena, SkillRegistry skills,
                         int arenaOriginX, int arenaOriginZ) {
        this(id, request, arena, skills, arenaOriginX, arenaOriginZ, BattleEffectResolver.logical());
    }
    public BattleSession(BattleId id, EncounterRequest request, ArenaGrid arena, SkillRegistry skills,
                         int arenaOriginX, int arenaOriginZ, BattleEffectResolver effects) {
        this.id = id; this.request = request; this.arena = arena; this.skills = skills;
        this.effects = effects;
        this.arenaOriginX = arenaOriginX; this.arenaOriginZ = arenaOriginZ;
        request.combatants().forEach(seed -> {
            Combatant combatant = new Combatant(seed, request.seed());
            if (!arena.place(combatant.id(), combatant.cell())) throw new IllegalArgumentException("Illegal or overlapping deployment cell: " + seed.cell());
            combatants.put(combatant.id(), combatant);
            if (combatant.playerId() != null) statistics.put(combatant.playerId(), new MutableStatistics());
        });
        factionOrder = combatants.values().stream().collect(Collectors.groupingBy(Combatant::factionId,
                        LinkedHashMap::new, Collectors.averagingDouble(Combatant::initiative)))
                .entrySet().stream().sorted(Map.Entry.<String, Double>comparingByValue().reversed()
                .thenComparing(Map.Entry::getKey)).map(Map.Entry::getKey).collect(Collectors.toCollection(ArrayList::new));
        prioritizeOpeningFaction();
        introProfile=skills.introProfile(request.introProfile());introTotalTicks = introProfile.totalTicks(factionOrder.size());
        transition(BattleState.LOADING_ARENA);
        transition(BattleState.TRANSFERRING);
        transition(BattleState.INTRO);
        phaseTicksRemaining = introTotalTicks;
        CombatReactionRules.ensureRegistered();
    }

    public BattleSession(BattleId id, EncounterRequest request, ArenaGrid arena, SkillRegistry skills,
                         int arenaOriginX, int arenaOriginZ, Collection<Combatant> restoredCombatants,
                         List<String> factionOrder, BattleState state, long revision, long eventSequence,
                         int round, int factionIndex, int phaseTicksRemaining, Set<UUID> readyPlayers) {
        this(id,request,arena,skills,arenaOriginX,arenaOriginZ,restoredCombatants,factionOrder,state,revision,eventSequence,round,factionIndex,phaseTicksRemaining,readyPlayers,BattleEffectResolver.logical());
    }
    public BattleSession(BattleId id, EncounterRequest request, ArenaGrid arena, SkillRegistry skills,
                         int arenaOriginX, int arenaOriginZ, Collection<Combatant> restoredCombatants,
                         List<String> factionOrder, BattleState state, long revision, long eventSequence,
                         int round, int factionIndex, int phaseTicksRemaining, Set<UUID> readyPlayers, BattleEffectResolver effects) {
        this.id = id; this.request = request; this.arena = arena; this.skills = skills; this.effects=effects;
        this.arenaOriginX = arenaOriginX; this.arenaOriginZ = arenaOriginZ;
        restoredCombatants.forEach(combatant -> {
            this.combatants.put(combatant.id(), combatant);
            if (!combatant.downed()) arena.place(combatant.id(), combatant.cell());
            if (combatant.playerId() != null) statistics.put(combatant.playerId(), new MutableStatistics());
        });
        this.factionOrder = new ArrayList<>(factionOrder);this.introProfile=skills.introProfile(request.introProfile()); this.introTotalTicks = introProfile.totalTicks(factionOrder.size());
        this.state = state; this.revision = revision; this.eventSequence = eventSequence;
        this.round = round; this.factionIndex = factionIndex; this.phaseTicksRemaining = phaseTicksRemaining; this.readyPlayers.addAll(readyPlayers);
        CombatReactionRules.ensureRegistered();
    }

    public BattleId id() { return id; }
    public EncounterRequest request() { return request; }
    public ArenaGrid arena() { return arena; }
    public Collection<Combatant> combatants() { return List.copyOf(combatants.values()); }
    public Optional<Combatant> combatant(UUID id) { return Optional.ofNullable(combatants.get(id)); }
    /** Mirrors live vanilla attributes into the read model without reviving a tactically downed unit. */
    public void syncLivingHealth(UUID id, float maximum, float current) {
        Combatant combatant = combatants.get(id);
        if (combatant == null || combatant.downed()) return;
        float previousMaximum = combatant.maxHealth(), previousHealth = combatant.health();
        float nextMaximum = Math.max(1, maximum);
        float nextCurrent = Math.min(nextMaximum, Math.max(0, current));
        if (Math.abs(combatant.maxHealth() - nextMaximum) < .001F
                && Math.abs(combatant.health() - nextCurrent) < .001F) return;
        combatant.syncHealth(nextMaximum, nextCurrent);
        eventBus.post(new BattleEvents.HealthSynchronized(this, combatant, previousMaximum, previousHealth, nextMaximum, nextCurrent));
        if (combatant.downed()) arena.remove(combatant.id());
        evaluateOutcome();
        changed();
    }
    public void applyExternalStatus(UUID targetId, BattleStatus status) {
        Combatant target=combatants.get(targetId); if (target == null) return;
        applyStatus(null, target, status);
    }
    /** Applies a status through the cancellable status event seam. */
    public boolean applyStatus(Combatant source, Combatant target, BattleStatus status) {
        boolean applied = applyStatusInternal(source, target, status);
        if (applied) changed();
        return applied;
    }
    public boolean replaceStatus(Combatant source, Combatant target, BattleStatus status) {
        if (target == null || target.downed() || status == null) return false;
        BattleEvents.StatusAboutToBeApplied about = new BattleEvents.StatusAboutToBeApplied(this, source, target, status);
        CombatReactionRules.onStatus(about);
        eventBus.post(about);
        if (about.cancelled()) return false;
        target.replaceStatus(about.status());
        appendEvent(new BattleEvent(eventSequence + 1, round, BattleEvent.Type.STATUS, target.id(), target.id(),
                target.name(), target.name(), about.status().id(), about.status().nameKey(), about.status().stacks(), target.cell(), target.cell()));
        eventBus.post(new BattleEvents.StatusApplied(this, source, target, about.status()));
        changed();
        return true;
    }
    public FactionRelation relationOf(String from, String to) { return relation(from, to); }
    public String activeFaction() { return factionOrder.isEmpty() ? "" : factionOrder.get(Math.min(factionIndex, factionOrder.size() - 1)); }
    public double hitHostiles(Combatant actor, Collection<BattleCell> cells, SkillDefinition definition, double amount,
                              BattleDamageType type, Collection<BattleCell> affected) {
        if (actor == null || cells == null || definition == null) return 0;
        Set<BattleCell> hitCells = new HashSet<>();
        cells.forEach(hitCells::add);
        double total = 0;
        for (Combatant target : List.copyOf(combatants.values())) {
            if (target.downed() || target.id().equals(actor.id())
                    || relation(actor.factionId(), target.factionId()) != FactionRelation.HOSTILE) continue;
            boolean inside = hitCells.stream().anyMatch(cell -> cell.x() == target.cell().x() && cell.z() == target.cell().z());
            if (!inside) continue;
            boolean wasDowned = target.downed();
            double applied = applyDamage(actor, target, definition.id(), amount, definition, type, affected);
            if (applied <= 0) continue;
            total += applied;
            recordEffect(actor, target, SkillDefinition.TargetType.ENEMY, applied);
            appendEvent(new BattleEvent(eventSequence + 1, round, BattleEvent.Type.DAMAGE, actor.id(), target.id(),
                    actor.name(), target.name(), definition.id(), definition.nameKey(), applied, actor.cell(), target.cell()));
            if (!wasDowned && target.downed()) { arena.remove(target.id()); appendDownedEvent(actor, target, definition); }
        }
        evaluateOutcome();
        return total;
    }
    public BattleState state() { return state; }
    public int factionIndex() { return factionIndex; }
    public Optional<SkillDefinition> skill(String id) { return skills.definition(id); }
    public Optional<SkillDefinition> skillForCard(UUID actorId, UUID cardInstanceId) {
        Combatant actor = combatants.get(actorId);
        if (actor == null) return Optional.empty();
        return actor.deck().card(cardInstanceId).flatMap(card -> resolvedDefinition(actor, card));
    }
    public BattleActionTimeline actions() { return actions; }
    public boolean hasLineOfSight(BattleCell from, BattleCell to, UUID actor, UUID target,
                                  boolean piercesUnits, boolean ignoresTerrain) {
        return arena.hasLineOfSight(from, to, actor, target, piercesUnits, ignoresTerrain,
                combatantId -> combatants.get(combatantId) == null || combatants.get(combatantId).blocksRangedLineOfSight());
    }
    public boolean actionBusy(UUID actorId) { return actions.moving(actorId) || actionLocks.getOrDefault(actorId, 0) > 0; }
    public boolean ready(Combatant actor) { return actor != null && actor.playerId() != null && readyPlayers.contains(actor.playerId()); }
    public int actionTicksRemaining(UUID actorId) {
        int skill = actionLocks.getOrDefault(actorId, 0);
        int movement = actions.moves().stream().filter(move -> move.actorId().equals(actorId))
                .mapToInt(move -> Math.max(0, move.durationTicks() - move.elapsedTicks())).findFirst().orElse(0);
        return Math.max(skill, movement);
    }
    public Map<UUID,Integer> actionLocks() { return Map.copyOf(actionLocks); }
    public Map<UUID, PendingDash> pendingDashes() { return Map.copyOf(pendingDashes); }
    public Optional<BattleResult> pendingResult() { return Optional.ofNullable(pendingResult); }

    /**
     * Finalizes result choices for participants that are no longer connected. Offline players must not
     * keep the remaining party on the result screen; their first candidate is the deterministic fallback.
     */
    public boolean autoSettleDisconnectedPlayers(Set<UUID> onlinePlayers) {
        if (state != BattleState.REWARD || rewards.isEmpty()) return false;
        boolean changed = false;
        for (var entry : rewards.entrySet()) {
            if (!onlinePlayers.contains(entry.getKey()) && !entry.getValue().confirmed()) {
                entry.setValue(entry.getValue().withFallback());
                changed = true;
            }
        }
        if (changed) changed();
        return changed;
    }
    public void onResult(Consumer<BattleResult> consumer) { resultConsumer = consumer; }
    public void onEvent(Consumer<BattleEvent> consumer) { eventConsumer = consumer; }
    public IEventBus events() { return eventBus; }
    public void itemAccess(BattleItemAccess access) { itemAccess = access == null ? BattleItemAccess.unavailable() : access; }
    public boolean syncActiveWeapon(Combatant actor, int slot) {
        if (actor == null) return false;
        int previous = actor.activeWeaponSlot();
        if (!actor.setActiveWeaponSlot(slot)) return false;
        BattleEvents.WeaponSwitched switched = new BattleEvents.WeaponSwitched(this, actor, previous, actor.activeWeaponSlot(), actor.activeWeaponItem());
        CombatReactionRules.onWeaponSwitched(switched);
        eventBus.post(switched);
        changed();
        return true;
    }
    public WeaponPassiveEngine passiveEngine() { return passiveEngine; }
    public void addDashCard(Combatant actor) { actor.deck().addToHandOrDraw("exworld:dash"); changed(); }
    public void addDashCard(Combatant actor, String passiveId) {
        addDashCard(actor);
        eventBus.post(new BattleEvents.PassiveRewardGranted(this, actor, passiveId, "exworld:dash"));
    }
    public void restoreEvents(Collection<BattleEvent> restored) {
        events.clear();
        restored.stream().skip(Math.max(0, restored.size() - 40L)).forEach(events::addLast);
    }

    public void restoreRuntime(Collection<BattleActionTimeline.MoveAction> moves, Map<UUID,Integer> restoredActionLocks, Set<UUID> introSkipped,
                               BattleResult restoredResult, Map<UUID, PersistedReward> restoredRewards,
                               Map<UUID, PersistedStatistics> restoredStatistics, double restoredPhaseDamage, long restoredBattleTicks) {
        restoreRuntime(moves, restoredActionLocks, introSkipped, restoredResult, restoredRewards, restoredStatistics, restoredPhaseDamage, restoredBattleTicks, Map.of());
    }
    public void restoreRuntime(Collection<BattleActionTimeline.MoveAction> moves, Map<UUID,Integer> restoredActionLocks, Set<UUID> introSkipped,
                               BattleResult restoredResult, Map<UUID, PersistedReward> restoredRewards,
                               Map<UUID, PersistedStatistics> restoredStatistics, double restoredPhaseDamage, long restoredBattleTicks,
                               Map<UUID, PendingDash> restoredDashes) {
        actions.restore(moves); moves.forEach(move -> arena.restoreReservation(move.actorId(), move.path()));
        pendingDashes.clear(); pendingDashes.putAll(restoredDashes);
        actionLocks.clear(); restoredActionLocks.forEach((actor,ticks) -> { if (ticks > 0) actionLocks.put(actor,ticks); });
        introSkippedPlayers.clear(); introSkippedPlayers.addAll(introSkipped);
        pendingResult = restoredResult; rewards.clear();
        restoredRewards.forEach((id, reward) -> rewards.put(id,
                new RewardState(reward.candidates(), reward.selected(), reward.confirmed(), reward.gold())));
        restoredStatistics.forEach((id, value) -> {
            MutableStatistics statistics = this.statistics.computeIfAbsent(id, ignored -> new MutableStatistics());
            statistics.damageDealt = value.damageDealt(); statistics.damageTaken = value.damageTaken();
            statistics.healing = value.healing(); statistics.cardsUsed = value.cardsUsed();
        });
        phaseDamage = Math.max(0, restoredPhaseDamage);
        battleTicks = Math.max(0, restoredBattleTicks);
    }
    public void restoreRuntime(Collection<BattleActionTimeline.MoveAction> moves, Set<UUID> introSkipped,
                               BattleResult restoredResult, Map<UUID, PersistedReward> restoredRewards,
                               Map<UUID, PersistedStatistics> restoredStatistics, double restoredPhaseDamage, long restoredBattleTicks) {
        restoreRuntime(moves, Map.of(), introSkipped, restoredResult, restoredRewards, restoredStatistics,
                restoredPhaseDamage, restoredBattleTicks);
    }

    public Map<UUID, PersistedReward> persistedRewards() {
        Map<UUID, PersistedReward> values = new LinkedHashMap<>();
        rewards.forEach((id, reward) -> values.put(id, new PersistedReward(reward.candidates(), reward.selected(), reward.confirmed(), reward.gold())));
        return Map.copyOf(values);
    }
    public Map<UUID, PersistedStatistics> persistedStatistics() {
        Map<UUID, PersistedStatistics> values = new LinkedHashMap<>();
        statistics.forEach((id, stat) -> values.put(id, new PersistedStatistics(stat.damageDealt, stat.damageTaken, stat.healing, stat.cardsUsed)));
        return Map.copyOf(values);
    }
    public Set<UUID> introSkippedPlayers() { return Set.copyOf(introSkippedPlayers); }
    public double phaseDamage() { return phaseDamage; }
    public long battleTicks() { return battleTicks; }
    public record PersistedReward(List<String> candidates, String selected, boolean confirmed, int gold) {
        public PersistedReward { candidates = List.copyOf(candidates); }
    }
    public record PersistedStatistics(double damageDealt, double damageTaken, double healing, int cardsUsed) {}
    public record PendingDash(List<UUID> targetIds) { public PendingDash { targetIds = List.copyOf(targetIds); } }

    private void resolveDash(Combatant actor, PendingDash dash) {
        if (dash == null) return;
        SkillDefinition basic = skills.definition("exworld:basic_attack").orElse(null);
        if (basic == null) return;
        for (UUID targetId : dash.targetIds()) {
            Combatant target = combatants.get(targetId);
            if (target == null || target.downed()) continue;
            double amount = applyDamage(actor, target, "exworld:dash", basic.power() + actor.strength(), basic);
            if (amount <= 0) continue;
            recordEffect(actor, target, SkillDefinition.TargetType.ENEMY, amount);
            appendEvent(new BattleEvent(eventSequence + 1, round, BattleEvent.Type.DAMAGE, actor.id(), target.id(),
                    actor.name(), target.name(), "exworld:dash", "skill.exworld.dash", amount, actor.cell(), target.cell()));
            if (target.downed()) { arena.remove(target.id()); appendDownedEvent(actor, target, basic); }
        }
        evaluateOutcome();
    }

    private double applyDamage(Combatant source, Combatant target, String causeId, double requested, SkillDefinition definition) {
        BattleDamageType type = definition != null && definition.physical() ? BattleDamageType.PHYSICAL : BattleDamageType.MAGIC;
        java.util.Collection<BattleCell> affected = target == null ? Set.of() : Set.of(target.cell());
        return applyDamage(source, target, causeId, requested, definition, type, affected);
    }

    private double applyDamage(Combatant source, Combatant target, String causeId, double requested, SkillDefinition definition,
                               BattleDamageType type, Collection<BattleCell> affected) {
        if (source == null || target == null || target.downed() || requested <= 0) return 0;
        BattleEvents.DamageAboutToBeDealt about = new BattleEvents.DamageAboutToBeDealt(this, source, target, causeId, requested,
                type, affected);
        CombatReactionRules.onDamage(about);
        eventBus.post(about);
        if (about.cancelled() || about.amount() <= 0) return 0;
        double applied = effects.damage(source, target, definition, about.amount());
        if (applied > 0) eventBus.post(new BattleEvents.DamageDealt(this, source, target, causeId, about.amount(), applied));
        return applied;
    }

    private double applyHealing(Combatant source, Combatant target, String causeId, double requested, SkillDefinition definition) {
        if (source == null || target == null || target.downed() || requested <= 0) return 0;
        BattleEvents.HealingAboutToBeApplied about = new BattleEvents.HealingAboutToBeApplied(this, source, target, causeId, requested);
        eventBus.post(about);
        if (about.cancelled() || about.amount() <= 0) return 0;
        double applied = effects.heal(source, target, definition, about.amount());
        if (applied > 0) eventBus.post(new BattleEvents.Healed(this, source, target, causeId, about.amount(), applied));
        return applied;
    }

    private boolean applyStatusInternal(Combatant source, Combatant target, BattleStatus status) {
        if (target == null || target.downed() || status == null) return false;
        BattleEvents.StatusAboutToBeApplied about = new BattleEvents.StatusAboutToBeApplied(this, source, target, status);
        CombatReactionRules.onStatus(about);
        eventBus.post(about);
        if (about.cancelled()) return false;
        target.addStatus(about.status());
        appendEvent(new BattleEvent(eventSequence + 1, round, BattleEvent.Type.STATUS, target.id(), target.id(),
                target.name(), target.name(), about.status().id(), about.status().nameKey(), about.status().stacks(), target.cell(), target.cell()));
        eventBus.post(new BattleEvents.StatusApplied(this, source, target, about.status()));
        return true;
    }

    /** Adapter seam for tactical displacement: reserve now, commit cell and world position at timeline completion. */
    public SkillResult scheduleDisplacement(Combatant actor, BattleCell destination) {
        if (actions.moving(actor.id())) return SkillResult.failure("battle.command.actor_busy");
        BattleCell halted = CombatReactionRules.haltSkillDestination(this, actor, actor.cell(), destination);
        if (halted.equals(actor.cell())) return SkillResult.success(0);
        BattleEvents.MoveAboutToStart about = new BattleEvents.MoveAboutToStart(this, actor, halted, actor.movementRemaining());
        eventBus.post(about);
        if (about.cancelled()) return SkillResult.failure(about.cancellationReason());
        Optional<List<BattleCell>> path = arena.reservePath(actor.id(), actor.cell(), halted, actor.movementRemaining());
        if (path.isEmpty() || path.get().isEmpty()) return SkillResult.failure("battle.command.no_path");
        BattleCell start = actor.cell(); actions.startMove(actor.id(), start, path.get());
        eventBus.post(new BattleEvents.MoveStarted(this, actor, start, halted, path.get(), true));
        appendEvent(new BattleEvent(eventSequence + 1, round, BattleEvent.Type.MOVE, actor.id(), null,
                actor.name(), "", "", "", path.get().size(), start, halted));
        return SkillResult.success(0);
    }

    public SkillResult scheduleDash(Combatant actor, BattleCell destination) {
        if (actions.moving(actor.id())) return SkillResult.failure("battle.command.actor_busy");
        BattleCell halted = CombatReactionRules.haltSkillDestination(this, actor, actor.cell(), destination);
        if (halted.equals(actor.cell())) return SkillResult.success(0);
        BattleEvents.MoveAboutToStart about = new BattleEvents.MoveAboutToStart(this, actor, halted, 4);
        eventBus.post(about);
        if (about.cancelled()) return SkillResult.failure(about.cancellationReason());
        Optional<List<BattleCell>> path = arena.reservePath(actor.id(), actor.cell(), halted, 4);
        if (path.isEmpty() || path.get().isEmpty()) return SkillResult.failure("battle.command.no_path");
        BattleCell start = actor.cell();
        actions.startFreeMove(actor.id(), start, path.get());
        eventBus.post(new BattleEvents.MoveStarted(this, actor, start, halted, path.get(), false));
        List<BattleCell> cells = new ArrayList<>(path.get()); cells.add(halted);
        Set<UUID> targetIds = new LinkedHashSet<>();
        for (BattleCell cell : cells) combatants.values().stream()
                .filter(target -> !target.id().equals(actor.id()) && !target.downed())
                .filter(target -> relation(actor.factionId(), target.factionId()) == FactionRelation.HOSTILE)
                .filter(target -> target.cell().distanceTo(cell) <= 1)
                .map(Combatant::id).forEach(targetIds::add);
        pendingDashes.put(actor.id(), new PendingDash(List.copyOf(targetIds)));
        return SkillResult.success(0);
    }

    /** Tactical teleport: validates and commits only the destination, without path animation or movement cost. */
    public SkillResult teleportDisplacement(Combatant actor, BattleCell destination) {
        if (actions.moving(actor.id())) return SkillResult.failure("battle.command.actor_busy");
        BattleCell start = actor.cell();
        if (start.equals(destination) || !arena.reserveDestination(actor.id(), destination))
            return SkillResult.failure("battle.command.no_path");
        arena.commit(actor.id(), destination); actor.relocate(destination);
        eventBus.post(new BattleEvents.Teleported(this, actor, start, destination));
        appendEvent(new BattleEvent(eventSequence + 1, round, BattleEvent.Type.MOVE, actor.id(), null,
                actor.name(), "", "exworld:teleport", "", 0, start, destination));
        return SkillResult.success(0);
    }

    /** Resolves a small tactical area hit without creating a normal action lock or spending movement. */
    public void damageHostilesAt(Combatant actor, BattleCell center, int radius, double baseAmount,
                                 String skillId, String nameKey) {
        SkillDefinition definition = skills.definition("exworld:basic_attack").orElse(null);
        if (definition == null || actor == null || center == null) return;
        for (Combatant target : List.copyOf(combatants.values())) {
            if (target.downed() || target.id().equals(actor.id())
                    || relation(actor.factionId(), target.factionId()) != FactionRelation.HOSTILE
                    || target.cell().distanceTo(center) > radius) continue;
            boolean wasDowned = target.downed();
            double amount = applyDamage(actor, target, skillId, Math.max(0, baseAmount) + actor.strength(), definition);
            if (amount <= 0) continue;
            recordEffect(actor, target, SkillDefinition.TargetType.ENEMY, amount);
            appendEvent(new BattleEvent(eventSequence + 1, round, BattleEvent.Type.DAMAGE, actor.id(), target.id(),
                    actor.name(), target.name(), skillId, nameKey, amount, center, target.cell()));
            if (!wasDowned && target.downed()) {
                arena.remove(target.id());
                appendDownedEvent(actor, target, definition);
            }
        }
        evaluateOutcome();
    }

    /** Registers a summon in the aggregate before a matching Minecraft entity is spawned. */
    public Optional<Combatant> summon(Combatant owner, BattleCell preferred, String name) {
        List<BattleCell> candidates = new ArrayList<>(); candidates.add(preferred);
        for (int radius=1;radius<=2;radius++) for(int dx=-radius;dx<=radius;dx++) for(int dz=-radius;dz<=radius;dz++)
            if(Math.max(Math.abs(dx),Math.abs(dz))==radius)candidates.add(new BattleCell(preferred.x()+dx,preferred.z()+dz,preferred.floorY()));
        for(BattleCell cell:candidates){
            if(cell.x()<0||cell.z()<0||cell.x()>=arena.definition().width()||cell.z()>=arena.definition().depth())continue;
            UUID id=UUID.randomUUID();
            EncounterRequest.CombatantSeed seed=new EncounterRequest.CombatantSeed(id,null,name,owner.factionId(),cell,
                    18,18,40,40,8,owner.initiative()-1,5,1,List.of("exworld:basic_attack"), List.of(1), -1,
                    net.exmo.exworld.battle.card.DeckState.DRAW_PER_PHASE, Set.of(net.exmo.exworld.battle.model.CombatantAttribute.AIRBORNE));
            Combatant summon=new Combatant(seed,request.seed());
            if(arena.place(id,cell)){combatants.put(id,summon);changed();eventBus.post(new BattleEvents.CombatantSummoned(this, owner, summon));return Optional.of(summon);}
        }
        return Optional.empty();
    }

    public boolean resolveExternalHit(UUID actorId, UUID targetId, String skillId, float amount) {
        Combatant actor = combatants.get(actorId), target = combatants.get(targetId);
        SkillDefinition definition = skills.definition(skillId).orElse(null);
        if (actor == null || target == null || definition == null || target.downed() || amount <= 0) return false;
        if (relation(actor.factionId(), target.factionId()) != FactionRelation.HOSTILE
                || !definition.reaches(actor.cell(), target.cell())) return false;
        boolean wasDowned = target.downed();
        double requested = skills.isDebug(skillId) ? amount * .4F : amount + actor.strength();
        double applied = applyDamage(actor, target, definition.id(), requested, definition);
        if (applied <= 0) return false;
        recordEffect(actor, target, SkillDefinition.TargetType.ENEMY, applied);
        appendEvent(new BattleEvent(eventSequence + 1, round, BattleEvent.Type.DAMAGE, actor.id(), target.id(),
                actor.name(), target.name(), definition.id(), definition.nameKey(), applied, actor.cell(), target.cell()));
        if (!wasDowned && target.downed()) { arena.remove(target.id()); appendDownedEvent(actor, target, definition); }
        evaluateOutcome(); changed(); return true;
    }

    public void tick() {
        battleTicks++;
        eventBus.post(new BattleEvents.Tick(this, battleTicks));
        if (!actionLocks.isEmpty()) {
            actionLocks.replaceAll((actor, ticks) -> ticks - 1);
            actionLocks.entrySet().removeIf(entry -> entry.getValue() <= 0);
            changed();
        }
        if (!actions.moves().isEmpty()) {
            List<BattleActionTimeline.MoveAction> completed = actions.tick();
            completed.forEach(move -> {
                Combatant actor = combatants.get(move.actorId());
                if (actor != null && !actor.downed()) {
                    arena.commit(actor.id(), move.destination());
                    if (move.consumesMovement()) actor.moveTo(move.destination(), move.path().size()); else actor.relocate(move.destination());
                    eventBus.post(new BattleEvents.MoveCompleted(this, actor, move.start(), move.destination(), move.path(), move.consumesMovement()));
                    PendingDash dash = pendingDashes.remove(actor.id());
                    resolveDash(actor, dash);
                    if (dash != null) appendEvent(new BattleEvent(eventSequence + 1, round, BattleEvent.Type.MOVE, actor.id(), null,
                            actor.name(), "", "exworld:dash", "skill.exworld.dash", move.path().size(), move.start(), move.destination()));
                }
                else arena.release(move.actorId());
            });
            changed();
        }
        if (state == BattleState.INTRO && (--phaseTicksRemaining <= 0 || allIntroSkipped())) beginDeployment();
        else if (state == BattleState.DEPLOYMENT && --phaseTicksRemaining <= 0) beginBattle();
        else if (state == BattleState.FACTION_PHASE) {
            boolean everyoneFinished = allPlayersReady() && !hasAutomatedCombatants();
            readyTicks = everyoneFinished ? readyTicks + 1 : 0;
            if ((--phaseTicksRemaining <= 0 || readyTicks >= READY_GRACE_TICKS) && !hasPendingActions()) resolvePhase();
        } else if (state == BattleState.RESOLVING && !hasPendingActions()) advanceFaction();
        else if (state.outcome() && --phaseTicksRemaining <= 0) beginReward();
        else if (state == BattleState.REWARD && (--phaseTicksRemaining <= 0 || allResultsConfirmed())) beginReturning();
        else if (state == BattleState.RETURNING && --phaseTicksRemaining <= 0) beginCleanup();
    }

    public void finishIntro() { if (state == BattleState.INTRO) beginDeployment(); }
    public void replayIntro() {
        if (state == BattleState.INTRO || state == BattleState.DEPLOYMENT) {
            introSkippedPlayers.clear(); transition(BattleState.INTRO); phaseTicksRemaining = introTotalTicks;
        }
    }
    public void openDebugResult(BattleResult.Outcome outcome) {
        if (pendingResult == null) finishDebug(outcome);
        if (state.outcome()) beginReward();
    }
    public void finishDeployment() {
        if (state == BattleState.INTRO) beginDeployment();
        if (state == BattleState.DEPLOYMENT) beginBattle();
    }
    public void finishActiveFaction() { if (state == BattleState.FACTION_PHASE) resolvePhase(); }
    public void finishDebug(BattleResult.Outcome outcome) {
        switch (outcome) {
            case VICTORY -> finish(BattleState.VICTORY, outcome);
            case DEFEAT -> finish(BattleState.DEFEAT, outcome);
            case ESCAPED -> finish(BattleState.ESCAPED, outcome);
            case ABORTED -> finish(BattleState.ABORTED, outcome);
        }
    }

    public CommandReceipt submit(BattleCommand command) {
        CommandReceipt duplicate = receipts.get(command.commandId());
        if (duplicate != null) return duplicate;
        if (!id.equals(command.battleId())) return remember(CommandReceipt.rejected(command.commandId(), "battle.command.wrong_session", revision));
        if (command.expectedRevision() > revision || command.expectedRevision() < 0)
            return remember(CommandReceipt.rejected(command.commandId(), "battle.command.stale_revision", revision));
        Combatant actor = combatants.get(command.actorId());
        if (actor == null) return remember(CommandReceipt.rejected(command.commandId(), "battle.command.unknown_actor", revision));
        if (actor.downed() && requiresStandingActor(command))
            return remember(CommandReceipt.rejected(command.commandId(), "battle.command.actor_downed", revision));

        BattleEvents.CommandAboutToExecute about = new BattleEvents.CommandAboutToExecute(this, command, actor);
        eventBus.post(about);
        if (about.cancelled()) {
            CommandReceipt rejected = CommandReceipt.rejected(command.commandId(), about.cancellationReason(), revision);
            eventBus.post(new BattleEvents.CommandRejected(this, command, actor, about.cancellationReason(), revision));
            return remember(rejected);
        }

        BattleEvents.ItemAboutToBeUsed itemAbout = command instanceof BattleCommand.UseItem use
                ? new BattleEvents.ItemAboutToBeUsed(this, actor, use.inventorySlot(), use.itemId(), use.targetCell()) : null;
        BattleEvents.WeaponSwitchAboutToStart weaponAbout = command instanceof BattleCommand.SwitchWeapon switchWeapon
                ? new BattleEvents.WeaponSwitchAboutToStart(this, actor, switchWeapon.weaponSlot()) : null;
        if (itemAbout != null) eventBus.post(itemAbout);
        if (weaponAbout != null) eventBus.post(weaponAbout);
        if (itemAbout != null && itemAbout.cancelled()) {
            String reason = itemAbout.cancellationReason();
            eventBus.post(new BattleEvents.ItemUseRejected(this, actor, itemAbout.inventorySlot(), itemAbout.itemId(), reason));
            eventBus.post(new BattleEvents.CommandRejected(this, command, actor, reason, revision));
            return remember(CommandReceipt.rejected(command.commandId(), reason, revision));
        }
        if (weaponAbout != null && weaponAbout.cancelled()) {
            eventBus.post(new BattleEvents.CommandRejected(this, command, actor, weaponAbout.cancellationReason(), revision));
            return remember(CommandReceipt.rejected(command.commandId(), weaponAbout.cancellationReason(), revision));
        }
        int previousWeaponSlot = actor.activeWeaponSlot();
        String failure = switch (command) {
            case BattleCommand.Move move -> move(actor, move.destination());
            case BattleCommand.UseSkill use -> useSkill(actor, use);
            case BattleCommand.SetReady ready -> setReady(actor, ready.ready());
            case BattleCommand.SetAutoBattle auto -> { actor.setAutoBattle(auto.enabled()); yield ""; }
            case BattleCommand.Escape ignored -> escape(actor);
            case BattleCommand.SkipIntro ignored -> skipIntro(actor);
            case BattleCommand.SelectReward reward -> selectReward(actor, reward.candidateId());
            case BattleCommand.ConfirmResult ignored -> confirmResult(actor);
            case BattleCommand.UseItem use -> itemAccess.useItem(this, actor, use);
            case BattleCommand.SwitchWeapon switchWeapon -> itemAccess.switchWeapon(this, actor, switchWeapon);
        };
        if (!failure.isEmpty()) {
            if (itemAbout != null) eventBus.post(new BattleEvents.ItemUseRejected(this, actor, itemAbout.inventorySlot(), itemAbout.itemId(), failure));
            eventBus.post(new BattleEvents.CommandRejected(this, command, actor, failure, revision));
            return remember(CommandReceipt.rejected(command.commandId(), failure, revision));
        }
        changed();
        if (itemAbout != null) eventBus.post(new BattleEvents.ItemUsed(this, actor, itemAbout.inventorySlot(), itemAbout.itemId(), itemAbout.targetCell()));
        if (weaponAbout != null) {
            BattleEvents.WeaponSwitched switched = new BattleEvents.WeaponSwitched(this, actor, previousWeaponSlot,
                    actor.activeWeaponSlot(), actor.activeWeaponItem());
            CombatReactionRules.onWeaponSwitched(switched);
            eventBus.post(switched);
        }
        CommandReceipt accepted = CommandReceipt.accepted(command.commandId(), revision);
        eventBus.post(new BattleEvents.CommandAccepted(this, command, actor, revision));
        return remember(accepted);
    }

    private static boolean requiresStandingActor(BattleCommand command) {
        return command instanceof BattleCommand.Move || command instanceof BattleCommand.UseSkill
                || command instanceof BattleCommand.SetReady || command instanceof BattleCommand.Escape;
    }

    public BattleSnapshot snapshot() {
        Map<UUID, BattleSnapshot.CombatantView> views = new LinkedHashMap<>();
        combatants.forEach((combatantId, combatant) -> views.put(combatantId, new BattleSnapshot.CombatantView(
                combatant.id(), combatant.playerId(), combatant.name(), combatant.factionId(), combatant.cell(), combatant.attributes(),
                combatant.health(), combatant.maxHealth(), combatant.block(), combatant.mana(), combatant.maxMana(), combatant.movementRemaining(), combatant.actionPoints(), combatant.actionPointsRemaining(),
                combatant.downed(), combatant.autoBattle(), combatant.deck().handWithInnate().stream()
                .filter(card -> !card.innate() || combatant.basicAttackAvailable())
                .map(card -> resolvedDefinition(combatant, card)
                        .map(definition -> new BattleSnapshot.CardView(card.instanceId(), card.skillId(), definition.nameKey(), descriptionKey(card, definition), definition.icon(),
                                card.innate(), card.star(), definition.manaCost(), definition.range(), definition.targetType(),
                                (cardDefinitionPlayable(card) && (!skills.adapter(definition.adapterId()).map(adapter -> adapter.requiresMana(this, combatant, definition)).orElse(true)
                                        || combatant.mana() >= definition.manaCost())
                                        && !(skills.card(card.skillId()).map(value -> value.type() == net.exmo.exworld.battle.card.CardDefinition.CardType.POWER && combatant.powerUsed(value.id())).orElse(false))), skills.retained(card.skillId()),
                                skills.card(card.skillId()).map(net.exmo.exworld.battle.card.CardDefinition::type).orElse(net.exmo.exworld.battle.card.CardDefinition.CardType.SKILL),
                                skills.card(card.skillId()).map(net.exmo.exworld.battle.card.CardDefinition::keywords).orElse(Set.of()), definition.chebyshevRange()))
                        .orElseGet(() -> new BattleSnapshot.CardView(card.instanceId(), card.skillId(), card.skillId(), card.skillId(), "",
                                card.innate(), card.star(), 0, 0, SkillDefinition.TargetType.CELL, false, false)))
                .toList(),
                pileViews(combatant.deck().drawPile()), pileViews(combatant.deck().discardPile()), combatant.statuses().stream()
                .map(status -> new BattleSnapshot.StatusView(status.id(), status.nameKey(), status.stacks(), status.remainingRounds(), status.beneficial())).toList(),
                combatant.itemUseLimit(), combatant.itemUsesRemaining(), combatant.activeWeaponSlot(),
                combatant.playerId() == null ? List.of() : itemAccess.weaponSlots(this, combatant),
                combatant.playerId() == null ? List.of() : itemAccess.items(combatant))));
        List<BattleSnapshot.MotionView> motions = actions.moves().stream().map(move -> new BattleSnapshot.MotionView(
                move.actorId(), move.start(), move.path(), move.durationTicks(), move.elapsedTicks())).toList();
        BattleSnapshot.IntroView intro = state == BattleState.INTRO ? new BattleSnapshot.IntroView(
                introTotalTicks - phaseTicksRemaining, introTotalTicks, introSkippedPlayers, playerIds().size()) : null;
        Map<UUID, BattleSnapshot.PlayerResultView> resultPlayers = new LinkedHashMap<>();
        rewards.forEach((playerId, reward) -> {
            MutableStatistics stat = statistics.getOrDefault(playerId, new MutableStatistics());
            resultPlayers.put(playerId, new BattleSnapshot.PlayerResultView(reward.candidates(), reward.selected(), reward.confirmed(),
                    reward.gold(), stat.damageDealt, stat.damageTaken, stat.healing, stat.cardsUsed));
        });
        BattleSnapshot.ResultView result = pendingResult == null ? null : new BattleSnapshot.ResultView(
                pendingResult.outcome().name(), phaseTicksRemaining, round, battleTicks, pendingResult.downed().size(), resultPlayers);
        return new BattleSnapshot(id, revision, eventSequence, state, request.arenaId(), arena.definition().width(), arenaOriginX, arenaOriginZ,
                arena.definition().blocked().stream().map(arena.definition()::cell).toList(),
                request.variables().getOrDefault("biome", "prairie"),
                round, activeFaction(), List.copyOf(factionOrder), phaseTicksRemaining, views, Set.copyOf(readyPlayers), List.copyOf(events),
                motions, intro, result, phaseDamage);
    }

    private List<BattleSnapshot.CardView> pileViews(List<SkillCard> cards) {
        return cards.stream().map(card -> resolvedDefinition(null, card).map(definition -> new BattleSnapshot.CardView(
                card.instanceId(),card.skillId(),definition.nameKey(),descriptionKey(card, definition),definition.icon(),card.innate(),card.star(),
                definition.manaCost(),definition.range(),definition.targetType(),false,skills.retained(card.skillId()),
                skills.card(card.skillId()).map(net.exmo.exworld.battle.card.CardDefinition::type).orElse(net.exmo.exworld.battle.card.CardDefinition.CardType.SKILL),
                skills.card(card.skillId()).map(net.exmo.exworld.battle.card.CardDefinition::keywords).orElse(Set.of()), definition.chebyshevRange()))
                .orElseGet(() -> new BattleSnapshot.CardView(card.instanceId(),card.skillId(),card.skillId(),card.skillId(),"",card.innate(),card.star(),0,0,SkillDefinition.TargetType.CELL,false,false))).toList();
    }

    private String move(Combatant actor, BattleCell destination) {
        if (!canAct(actor)) return "battle.command.not_active_faction";
        if (actionBusy(actor.id())) return "battle.command.actor_busy";
        if (!actor.hasActionPoint()) return "battle.command.no_action_points";
        BattleEvents.MoveAboutToStart about = new BattleEvents.MoveAboutToStart(this, actor, destination, actor.movementRemaining());
        eventBus.post(about);
        if (about.cancelled()) return about.cancellationReason();
        Optional<List<BattleCell>> path = arena.reservePath(actor.id(), actor.cell(), destination, actor.movementRemaining());
        if (path.isEmpty()) return "battle.command.no_path";
        BattleCell start = actor.cell();
        List<BattleCell> used = path.get();
        CombatReactionRules.Intercept intercept = CombatReactionRules.interceptOrdinaryMove(this, actor, start, used).orElse(null);
        if (intercept != null) {
            arena.release(actor.id());
            CombatReactionRules.displaceFree(this, intercept.interceptor(), intercept.intersection());
            used = intercept.truncatedPath();
            if (!used.isEmpty()) {
                Optional<List<BattleCell>> reserved = arena.reservePath(actor.id(), start, used.getLast(), used.size());
                used = reserved.orElse(List.of());
            }
        }
        actor.consumeActionPoint();
        if (!used.isEmpty()) {
            actions.startMove(actor.id(), start, used);
            eventBus.post(new BattleEvents.MoveStarted(this, actor, start, used.getLast(), used, true));
            appendEvent(new BattleEvent(eventSequence + 1, round, BattleEvent.Type.MOVE, actor.id(), null,
                    actor.name(), "", "", "", used.size(), start, used.getLast()));
        }
        if (intercept != null && intercept.damage() > 0) {
            CombatReactionRules.beginWave(this);
            SkillDefinition basic = skills.definition("exworld:basic_attack").orElse(null);
            applyDamage(actor, intercept.interceptor(), "exworld:intercept", intercept.damage(), basic,
                    BattleDamageType.PHYSICAL, Set.of(intercept.interceptor().cell()));
            CombatReactionRules.clearWave(this);
        }
        return "";
    }

    private String useSkill(Combatant actor, BattleCommand.UseSkill command) {
        if (!canAct(actor)) return "battle.command.not_active_faction";
        if (actionBusy(actor.id())) return "battle.command.actor_busy";
        if (!actor.hasActionPoint()) return "battle.command.no_action_points";
        Optional<SkillCard> cardValue = actor.deck().card(command.cardInstanceId());
        if (cardValue.isEmpty()) return "battle.command.card_not_in_hand";
        SkillCard card = cardValue.get();
        if (card.innate() && !actor.basicAttackAvailable()) return "battle.command.card_already_used";
        net.exmo.exworld.battle.card.CardDefinition cardDefinition = skills.card(card.skillId()).orElse(null);
        if (cardDefinition != null && !cardDefinition.playable()) return "battle.command.card_unplayable";
        if (cardDefinition != null && cardDefinition.type() == net.exmo.exworld.battle.card.CardDefinition.CardType.POWER
                && actor.powerUsed(cardDefinition.id())) return "battle.command.power_already_used";
        Optional<SkillDefinition> definitionValue = resolvedDefinition(actor, card);
        if (definitionValue.isEmpty()) return "battle.command.unknown_skill";
        SkillDefinition definition = definitionValue.get();
        Combatant target = command.targetId() == null ? null : combatants.get(command.targetId());
        BattleCell targetCell = command.targetCell() != null ? command.targetCell() : target == null ? actor.cell() : target.cell();
        if (!definition.reaches(actor.cell(), targetCell)) return "battle.command.out_of_range";
        String targetFailure = validateTarget(actor, target, definition.targetType());
        if (!targetFailure.isEmpty()) return targetFailure;
        if (definition.requiresLineOfSight() && !hasLineOfSight(actor.cell(), targetCell, actor.id(),
                target == null ? null : target.id(), definition.piercesUnits(), definition.ignoresTerrain())) return "battle.command.no_line_of_sight";
        Optional<SkillAdapter> adapter = skills.adapter(definition.adapterId());
        if (adapter.isEmpty()) return "battle.command.missing_adapter";
        if (adapter.get().requiresMana(this, actor, definition) && actor.mana() < definition.manaCost())
            return "battle.command.insufficient_mana";
        BattleEvents.CardAboutToPlay about = new BattleEvents.CardAboutToPlay(this, actor, card, cardDefinition, definition, target, targetCell);
        eventBus.post(about);
        if (about.cancelled()) return about.cancellationReason();
        CombatReactionRules.beginWave(this);
        SkillUse use = new SkillUse(this, actor, target, targetCell, card, cardDefinition, definition);
        SkillResult result = adapter.get().execute(use);
        if (!result.success()) { CombatReactionRules.clearWave(this); return result.reason(); }
        if (definition.id().equals("exworld:intercept") || definition.id().equals("exworld:taunt")) actor.spendAllActionPoints();
        else actor.consumeActionPoint();
        if (cardDefinition != null && cardDefinition.type() == net.exmo.exworld.battle.card.CardDefinition.CardType.POWER)
            actor.markPowerUsed(cardDefinition.id());
        int actionTicks = Math.max(1, adapter.get().actionTicks(use, result));
        actionLocks.put(actor.id(), actionTicks);
        boolean targetWasDowned = target != null && target.downed();
        double displayedAmount = 0;
        if (result.amount() > 0 && target != null) {
            displayedAmount = definition.targetType() == SkillDefinition.TargetType.ENEMY
                    ? applyDamage(actor, target, definition.id(), result.amount(), definition,
                    definition.physical() ? BattleDamageType.PHYSICAL : BattleDamageType.MAGIC, Set.of(target.cell()))
                    : definition.targetType() == SkillDefinition.TargetType.CELL ? 0
                    : applyHealing(actor, target, definition.id(), result.amount(), definition);
        }
        if (definition.id().equals(BattleSkillIds.BASIC_STRIKE)
                && actor.weaponFamily() == net.exmo.exworld.battle.weapon.WeaponFamily.HEAVY)
            replaceStatus(actor, actor, new BattleStatus(BattleSkillIds.EXHAUSTION, "status.exworld.exhaustion", 1, 1, true));
        if (result.consumeCard()) recordCardPlayed(actor);
        if (result.consumeCard() && cardDefinition != null && cardDefinition.type() == net.exmo.exworld.battle.card.CardDefinition.CardType.ATTACK)
            passiveEngine.onAttackCard(this, actor);
        else if (result.consumeCard() && card.innate()) passiveEngine.onAttackCard(this, actor);
        recordEffect(actor, target, definition.targetType(), displayedAmount);
        if (result.chargeMana()) actor.spendMana(definition.manaCost());
        if (definition.id().equals("exworld:quick_meditation"))
            actor.restoreMana(Math.max(10F, actor.maxMana() * 0.05F));
        if (result.consumeCard()) {
            if (card.innate()) actor.consumeBasicAttack(); else actor.deck().consume(card.instanceId(), cardDefinition != null && cardDefinition.has(net.exmo.exworld.battle.card.CardDefinition.CardKeyword.EXHAUST));
        }
        if (target != null && target.downed()) arena.remove(target.id());
        appendEvent(new BattleEvent(eventSequence + 1, round, BattleEvent.Type.SKILL, actor.id(), target == null ? null : target.id(),
                actor.name(), target == null ? "" : target.name(), definition.id(), definition.nameKey(), 0,
                actor.cell(), targetCell));
        if (displayedAmount > 0 && target != null) appendEvent(new BattleEvent(eventSequence + 1, round,
                definition.targetType() == SkillDefinition.TargetType.ENEMY ? BattleEvent.Type.DAMAGE : BattleEvent.Type.HEAL,
                actor.id(), target.id(), actor.name(), target.name(), definition.id(), definition.nameKey(), displayedAmount,
                actor.cell(), targetCell));
        if (target != null && !targetWasDowned && target.downed()) appendDownedEvent(actor, target, definition);
        eventBus.post(new BattleEvents.CardPlayed(this, actor, card, definition, target, targetCell, displayedAmount, result.consumeCard()));
        eventBus.post(new BattleEvents.SkillResolved(this, actor, definition, target, displayedAmount));
        evaluateOutcome();
        return "";
    }

    private String setReady(Combatant actor, boolean ready) {
        boolean activePlayer = actor.playerControlled() && state == BattleState.FACTION_PHASE
                && actor.factionId().equals(activeFaction());
        if (!activePlayer || (ready && !canAct(actor))) return "battle.command.not_active_player";
        if (ready) readyPlayers.add(actor.playerId());
        else { readyPlayers.remove(actor.playerId()); readyTicks = 0; }
        return "";
    }

    private String escape(Combatant actor) {
        if (!actor.playerControlled() || state != BattleState.FACTION_PHASE) return "battle.command.cannot_escape";
        finish(BattleState.ESCAPED, BattleResult.Outcome.ESCAPED);
        return "";
    }

    private String skipIntro(Combatant actor) {
        if (!actor.playerControlled() || state != BattleState.INTRO) return "battle.command.cannot_skip_intro";
        introSkippedPlayers.add(actor.playerId());
        return "";
    }

    private String selectReward(Combatant actor, String candidateId) {
        RewardState reward = actor.playerId() == null ? null : rewards.get(actor.playerId());
        if (state != BattleState.REWARD || reward == null || !reward.candidates().contains(candidateId))
            return "battle.command.invalid_reward";
        rewards.put(actor.playerId(), reward.select(candidateId));
        return "";
    }

    private String confirmResult(Combatant actor) {
        RewardState reward = actor.playerId() == null ? null : rewards.get(actor.playerId());
        if (state != BattleState.REWARD || reward == null) return "battle.command.result_unavailable";
        if (!reward.candidates().isEmpty() && reward.selected() == null) return "battle.command.reward_not_selected";
        rewards.put(actor.playerId(), reward.confirm());
        return "";
    }

    private String validateTarget(Combatant actor, Combatant target, SkillDefinition.TargetType type) {
        if (type == SkillDefinition.TargetType.CELL) return "";
        if (type == SkillDefinition.TargetType.SELF) return target == actor ? "" : "battle.command.target_must_be_self";
        if (target == null || target.downed()) return "battle.command.invalid_target";
        FactionRelation relation = relation(actor.factionId(), target.factionId());
        if (type == SkillDefinition.TargetType.ENEMY && relation != FactionRelation.HOSTILE) return "battle.command.target_not_hostile";
        if (type == SkillDefinition.TargetType.ALLY && relation != FactionRelation.FRIENDLY) return "battle.command.target_not_friendly";
        return "";
    }

    private boolean canAct(Combatant actor) {
        return state == BattleState.FACTION_PHASE && actor.factionId().equals(activeFaction())
                && (actor.playerId() == null || !readyPlayers.contains(actor.playerId()));
    }
    private boolean hasPendingActions() { return !actions.moves().isEmpty() || !actionLocks.isEmpty(); }

    private void beginBattle() {
        if (factionOrder.isEmpty()) { finish(BattleState.ABORTED, BattleResult.Outcome.ABORTED); return; }
        round = 1; factionIndex = 0;
        transition(BattleState.FACTION_PHASE); beginActiveFaction();
        eventBus.post(new BattleEvents.BattleStarted(this, round, activeFaction()));
    }

    private void prioritizeOpeningFaction() {
        int requestedOpening = request.openingFaction() == null ? -1 : factionOrder.indexOf(request.openingFaction());
        if (requestedOpening > 0 && factionAlive(request.openingFaction())) Collections.rotate(factionOrder, -requestedOpening);
    }

    private void beginDeployment() {
        transition(BattleState.DEPLOYMENT);
        phaseTicksRemaining = configuredDeploymentTicks();
        // World-hit encounters explicitly use zero because they have no placement input. Map encounters
        // retain the default deployment window unless their request supplies another duration.
        if (phaseTicksRemaining <= 0) beginBattle();
    }

    private int configuredDeploymentTicks() {
        String configured = request.variables().get("deployment_ticks");
        if (configured == null || configured.isBlank()) return DEFAULT_DEPLOYMENT_TICKS;
        try { return Math.max(0, Integer.parseInt(configured)); }
        catch (NumberFormatException ignored) { return DEFAULT_DEPLOYMENT_TICKS; }
    }

    private void resolvePhase() {
        eventBus.post(new BattleEvents.PhaseEnding(this, round, activeFaction()));
        combatants.values().stream().filter(c -> c.factionId().equals(activeFaction()) && !c.downed())
                .forEach(combatant -> combatant.deck().endPhase(card -> skills.retained(card.skillId()),
                        card -> skills.card(card.skillId()).map(value -> value.has(net.exmo.exworld.battle.card.CardDefinition.CardKeyword.ETHEREAL)).orElse(false)));
        transition(BattleState.RESOLVING);
        eventBus.post(new BattleEvents.PhaseEnded(this, round, activeFaction()));
    }
    private void advanceFaction() {
        evaluateOutcome();
        if (state != BattleState.RESOLVING) return;
        int checked = 0;
        do {
            factionIndex++;
            if (factionIndex >= factionOrder.size()) { factionIndex = 0; round++; }
            checked++;
        } while (checked <= factionOrder.size() && !factionAlive(activeFaction()));
        if (checked > factionOrder.size()) { finish(BattleState.ABORTED, BattleResult.Outcome.ABORTED); return; }
        transition(BattleState.FACTION_PHASE); beginActiveFaction();
    }

    private void beginActiveFaction() {
        readyPlayers.clear(); readyTicks = 0; phaseDamage = 0; phaseTicksRemaining = DEFAULT_PHASE_TICKS;
        eventBus.post(new BattleEvents.PhaseStarting(this, round, activeFaction(), factionIndex));
        combatants.values().stream().filter(c -> c.factionId().equals(activeFaction()) && !c.downed()).forEach(combatant -> {
            resolveTurnStatuses(combatant); combatant.beginPhase();
        });
        evaluateOutcome();
        changed();
        eventBus.post(new BattleEvents.PhaseStarted(this, round, activeFaction(), factionIndex));
    }

    private void resolveTurnStatuses(Combatant target) {
        for (BattleStatus status : target.statuses()) {
            float amount = switch (status.id()) {
                case "minecraft:poison" -> Math.min(Math.max(0,target.health()-1), 2F*status.stacks());
                case "minecraft:wither" -> 2F*status.stacks();
                case "minecraft:regeneration" -> -2F*status.stacks();
                default -> 0;
            };
            if(amount>0){double applied=applyDamage(target, target, status.id(), amount, skills.definition("exworld:basic_attack").orElseThrow());if(applied>0)appendEvent(new BattleEvent(eventSequence+1,round,BattleEvent.Type.DAMAGE,target.id(),target.id(),target.name(),target.name(),status.id(),status.nameKey(),applied,target.cell(),target.cell()));}
            else if(amount<0){double applied=applyHealing(target, target, status.id(), -amount, skills.definition("exworld:basic_attack").orElseThrow());if(applied>0)appendEvent(new BattleEvent(eventSequence+1,round,BattleEvent.Type.HEAL,target.id(),target.id(),target.name(),target.name(),status.id(),status.nameKey(),applied,target.cell(),target.cell()));}
            if(target.downed()){
                arena.remove(target.id());
                appendEvent(new BattleEvent(eventSequence+1,round,BattleEvent.Type.DOWNED,target.id(),target.id(),
                        target.name(),target.name(),status.id(),status.nameKey(),0,target.cell(),target.cell()));
                eventBus.post(new BattleEvents.CombatantDowned(this, target, target, status.id()));
                break;
            }
        }
    }

    private boolean allPlayersReady() {
        List<UUID> players = combatants.values().stream().filter(c -> canFactionParticipate(c.factionId()) && c.playerControlled() && !c.downed())
                .map(Combatant::playerId).distinct().toList();
        return !players.isEmpty() && readyPlayers.containsAll(players);
    }
    private boolean hasAutomatedCombatants() {
        return combatants.values().stream().anyMatch(combatant -> !combatant.downed()
                && combatant.factionId().equals(activeFaction())
                && (!combatant.playerControlled() || combatant.autoBattle()));
    }
    private boolean allIntroSkipped() { return !playerIds().isEmpty() && introSkippedPlayers.containsAll(playerIds()); }
    private boolean allResultsConfirmed() { return !rewards.isEmpty() && rewards.values().stream().allMatch(RewardState::confirmed); }
    private Set<UUID> playerIds() { return combatants.values().stream().map(Combatant::playerId).filter(Objects::nonNull).collect(Collectors.toSet()); }

    private boolean canFactionParticipate(String faction) { return faction.equals(activeFaction()); }
    private boolean factionAlive(String faction) { return combatants.values().stream().anyMatch(c -> c.factionId().equals(faction) && !c.downed()); }
    private String descriptionKey(SkillDefinition definition) {
        if (definition.id().startsWith("exworld:debug_card_")) {
            int number = Integer.parseInt(definition.id().substring(definition.id().length() - 2));
            return "skill.exworld.debug_family_" + ((number - 1) % 4 + 1) + ".description";
        }
        return definition.nameKey().startsWith("spell.") ? definition.nameKey() + ".guide" : definition.nameKey() + ".description";
    }
    private String descriptionKey(SkillCard card, SkillDefinition definition) {
        return skills.card(card.skillId()).map(net.exmo.exworld.battle.card.CardDefinition::descriptionKey).filter(value -> value != null && !value.isBlank())
                .orElseGet(() -> descriptionKey(definition));
    }
    private Optional<SkillDefinition> resolvedDefinition(SkillCard card) {
        return resolvedDefinition(null, card);
    }
    private Optional<SkillDefinition> resolvedDefinition(Combatant actor, SkillCard card) {
        try {
            SkillDefinition base = skills.resolve(card.skillId(), card.star());
            if (actor == null) return Optional.of(base);
            return Optional.of(skills.adapter(base.adapterId()).map(adapter -> adapter.resolveForActor(this, actor, base)).orElse(base));
        }
        catch (IllegalArgumentException ignored) { return Optional.empty(); }
    }
    private boolean cardDefinitionPlayable(SkillCard card) {
        return skills.card(card.skillId()).map(net.exmo.exworld.battle.card.CardDefinition::playable).orElse(true);
    }
    private FactionRelation relation(String from, String to) {
        if (from.equals(to)) return FactionRelation.FRIENDLY;
        return request.relations().getOrDefault(new EncounterRequest.FactionPair(from, to), FactionRelation.HOSTILE);
    }

    private void evaluateOutcome() {
        Set<String> playerFactions = combatants.values().stream().filter(Combatant::playerControlled).map(Combatant::factionId).collect(Collectors.toSet());
        boolean playerAlive = combatants.values().stream().anyMatch(c -> playerFactions.contains(c.factionId()) && !c.downed());
        if (!playerAlive) { finish(BattleState.DEFEAT, BattleResult.Outcome.DEFEAT); return; }
        boolean hostileAlive = combatants.values().stream().anyMatch(candidate -> !candidate.downed()
                && playerFactions.stream().anyMatch(playerFaction -> relation(playerFaction, candidate.factionId()) == FactionRelation.HOSTILE));
        if (!hostileAlive) finish(BattleState.VICTORY, BattleResult.Outcome.VICTORY);
    }

    private void finish(BattleState next, BattleResult.Outcome outcome) {
        if (state.outcome() || state == BattleState.REWARD || state == BattleState.RETURNING || state.terminal()) return;
        transition(next);
        Set<UUID> downed = combatants.values().stream().filter(Combatant::downed).map(Combatant::id).collect(Collectors.toSet());
        Set<UUID> survivors = combatants.values().stream().filter(c -> !c.downed()).map(Combatant::id).collect(Collectors.toSet());
        pendingResult = new BattleResult(id, outcome, survivors, downed, Set.of("elimination"), request.variables());
        phaseTicksRemaining = OUTCOME_PRESENTATION_TICKS;
        eventBus.post(new BattleEvents.BattleOutcome(this, pendingResult));
    }

    private void beginReward() {
        transition(BattleState.REWARD);
        phaseTicksRemaining = RESULT_TIMEOUT_TICKS;
        boolean victory = pendingResult != null && pendingResult.outcome() == BattleResult.Outcome.VICTORY;
        List<String> catalog = skills.standardCardIds().stream().filter(id -> skills.card(id).map(net.exmo.exworld.battle.card.CardDefinition::playable).orElse(false)).toList(); var rewardPool=request.rewardPool()==null?null:skills.rewardPool(request.rewardPool()).orElse(null);
        int offset = catalog.isEmpty() ? 0 : Math.floorMod((int) request.seed(), catalog.size());
        for (UUID playerId : playerIds()) {
            List<String> candidates = !victory?List.of():rewardPool!=null?rewardPool.choose(request.seed()^playerId.getLeastSignificantBits(),3):!catalog.isEmpty()?java.util.stream.IntStream.range(0, Math.min(3, catalog.size())).mapToObj(i -> catalog.get((offset + i) % catalog.size())).toList():List.of();
            rewards.put(playerId, new RewardState(candidates, null, false, victory ? rewardPool==null?100:rewardPool.gold() : 0));
        }
    }

    private void beginReturning() {
        rewards.replaceAll((id, reward) -> reward.withFallback());
        transition(BattleState.RETURNING);
        phaseTicksRemaining = RETURNING_TICKS;
    }

    private void beginCleanup() {
        transition(BattleState.CLEANUP);
        if (!resultPublished && pendingResult != null) {
            Map<UUID,BattleResult.PlayerSummary> stats=new LinkedHashMap<>();statistics.forEach((id,value)->stats.put(id,new BattleResult.PlayerSummary(value.damageDealt,value.damageTaken,value.healing,value.cardsUsed)));
            Map<UUID,BattleResult.RewardSummary> resultRewards=new LinkedHashMap<>();rewards.forEach((id,value)->resultRewards.put(id,new BattleResult.RewardSummary(value.selected(),value.gold(),true)));
            pendingResult=new BattleResult(pendingResult.battleId(),pendingResult.outcome(),pendingResult.survivors(),pendingResult.downed(),pendingResult.completedObjectives(),pendingResult.resultFlags(),stats,resultRewards);
            resultPublished = true;
            resultConsumer.accept(pendingResult);
            eventBus.post(new BattleEvents.ResultPublished(this, pendingResult));
        }
    }

    private void recordEffect(Combatant actor, Combatant target, SkillDefinition.TargetType type, double amount) {
        if (amount <= 0) return;
        MutableStatistics actorStats = actor.playerId() == null ? null : statistics.get(actor.playerId());
        if (type == SkillDefinition.TargetType.ENEMY) {
            phaseDamage += amount;
            if (actorStats != null) actorStats.damageDealt += amount;
            if (target != null && target.playerId() != null) statistics.get(target.playerId()).damageTaken += amount;
        } else if (actorStats != null) actorStats.healing += amount;
    }
    private void recordCardPlayed(Combatant actor) {
        MutableStatistics value = actor.playerId() == null ? null : statistics.get(actor.playerId());
        if (value != null) value.cardsUsed++;
    }

    private static final class MutableStatistics {
        private double damageDealt, damageTaken, healing;
        private int cardsUsed;
    }
    private record RewardState(List<String> candidates, String selected, boolean confirmed, int gold) {
        private RewardState { candidates = List.copyOf(candidates); }
        private RewardState select(String value) { return new RewardState(candidates, value, confirmed, gold); }
        private RewardState confirm() { return new RewardState(candidates, selected, true, gold); }
        private RewardState withFallback() { return new RewardState(candidates, selected == null && !candidates.isEmpty() ? candidates.getFirst() : selected, true, gold); }
    }

    private CommandReceipt remember(CommandReceipt receipt) {
        if (receipts.size() >= 1024) receipts.remove(receipts.keySet().iterator().next());
        receipts.put(receipt.commandId(), receipt); return receipt;
    }
    private void appendDownedEvent(Combatant actor, Combatant target, SkillDefinition definition) {
        appendEvent(new BattleEvent(eventSequence + 1, round, BattleEvent.Type.DOWNED, actor.id(), target.id(),
                actor.name(), target.name(), definition.id(), definition.nameKey(), 0, actor.cell(), target.cell()));
        eventBus.post(new BattleEvents.CombatantDowned(this, actor, target, definition.id()));
    }
    private void appendEvent(BattleEvent event) {
        if (events.size() >= 100) events.removeFirst();
        events.addLast(event);
        eventConsumer.accept(event);
        eventBus.post(new BattleEvents.Presentation(this, event));
    }
    private void transition(BattleState next) {
        BattleState previous = state; state = next; changed();
        if (previous != next) eventBus.post(new BattleEvents.StateChanged(this, previous, next));
    }
    private void changed() { revision++; eventSequence++; }
}
