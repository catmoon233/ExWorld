package net.exmo.exworld.battle.api.event;

import net.exmo.exworld.battle.BattleSession;
import net.exmo.exworld.battle.api.BattleCommand;
import net.exmo.exworld.battle.api.BattleEvent;
import net.exmo.exworld.battle.api.BattleResult;
import net.exmo.exworld.battle.card.CardDefinition;
import net.exmo.exworld.battle.card.SkillCard;
import net.exmo.exworld.battle.combatant.Combatant;
import net.exmo.exworld.battle.model.BattleCell;
import net.exmo.exworld.battle.model.BattleState;
import net.exmo.exworld.battle.model.BattleStatus;
import net.exmo.exworld.battle.skill.SkillDefinition;
import net.neoforged.bus.api.ICancellableEvent;

import java.util.List;

/**
 * Public semantic events emitted by a BattleSession.
 *
 * <p>Every event is a native NeoForge event and is posted directly to
 * {@code NeoForge.EVENT_BUS}. Consumers can subscribe with NeoForge's typed
 * listener API, priorities, and cancellation support. Presentation
 * {@link BattleEvent}s remain available in snapshots.</p>
 */
public final class BattleEvents {
    private BattleEvents() {}

    public abstract static class Event extends net.neoforged.bus.api.Event {
        private final BattleSession session;

        protected Event(BattleSession session) {
            this.session = session;
        }

        public final BattleSession session() {
            return session;
        }
    }

    public abstract static class Cancellable extends Event implements ICancellableEvent {
        private String cancellationReason = "battle.event.cancelled";
        private boolean reactionsApplied;

        protected Cancellable(BattleSession session) {
            super(session);
        }

        public final boolean cancelled() {
            return isCanceled();
        }

        public final void cancel() {
            cancel("battle.event.cancelled");
        }

        public final void cancel(String reason) {
            if (reason != null && !reason.isBlank()) cancellationReason = reason;
            setCanceled(true);
        }

        public final String cancellationReason() {
            return cancellationReason;
        }

        public final boolean reactionsApplied() { return reactionsApplied; }
        public final void markReactionsApplied() { reactionsApplied = true; }
    }

    public abstract static class CombatantEvent extends Event {
        private final Combatant actor;

        protected CombatantEvent(BattleSession session, Combatant actor) {
            super(session);
            this.actor = actor;
        }

        public final Combatant actor() {
            return actor;
        }
    }

    public abstract static class TargetedEvent extends CombatantEvent {
        private final Combatant target;

        protected TargetedEvent(BattleSession session, Combatant actor, Combatant target) {
            super(session, actor);
            this.target = target;
        }

        public final Combatant target() {
            return target;
        }
    }

    public static final class CommandAboutToExecute extends Cancellable {
        private final BattleCommand command;
        private final Combatant actor;

        public CommandAboutToExecute(BattleSession session, BattleCommand command, Combatant actor) {
            super(session);
            this.command = command;
            this.actor = actor;
        }

        public BattleCommand command() { return command; }
        public Combatant actor() { return actor; }
    }

    public static final class CommandAccepted extends Event {
        private final BattleCommand command;
        private final Combatant actor;
        private final long revision;

        public CommandAccepted(BattleSession session, BattleCommand command, Combatant actor, long revision) {
            super(session); this.command = command; this.actor = actor; this.revision = revision;
        }

        public BattleCommand command() { return command; }
        public Combatant actor() { return actor; }
        public long revision() { return revision; }
    }

    public static final class CommandRejected extends Event {
        private final BattleCommand command;
        private final Combatant actor;
        private final String reason;
        private final long revision;

        public CommandRejected(BattleSession session, BattleCommand command, Combatant actor, String reason, long revision) {
            super(session); this.command = command; this.actor = actor; this.reason = reason; this.revision = revision;
        }

        public BattleCommand command() { return command; }
        public Combatant actor() { return actor; }
        public String reason() { return reason; }
        public long revision() { return revision; }
    }

    public static final class Tick extends Event {
        private final long battleTicks;

        public Tick(BattleSession session, long battleTicks) { super(session); this.battleTicks = battleTicks; }
        public long battleTicks() { return battleTicks; }
    }

    public static final class StateChanged extends Event {
        private final BattleState previous;
        private final BattleState current;

        public StateChanged(BattleSession session, BattleState previous, BattleState current) {
            super(session); this.previous = previous; this.current = current;
        }

        public BattleState previous() { return previous; }
        public BattleState current() { return current; }
    }

    public static final class BattleStarted extends Event {
        private final int round;
        private final String activeFaction;

        public BattleStarted(BattleSession session, int round, String activeFaction) {
            super(session); this.round = round; this.activeFaction = activeFaction;
        }

        public int round() { return round; }
        public String activeFaction() { return activeFaction; }
    }

    public static final class BattleOutcome extends Event {
        private final BattleResult result;

        public BattleOutcome(BattleSession session, BattleResult result) { super(session); this.result = result; }
        public BattleResult result() { return result; }
    }

    public static final class ResultPublished extends Event {
        private final BattleResult result;

        public ResultPublished(BattleSession session, BattleResult result) { super(session); this.result = result; }
        public BattleResult result() { return result; }
    }

    public static final class PhaseStarting extends Event {
        private final int round;
        private final String faction;
        private final int factionIndex;

        public PhaseStarting(BattleSession session, int round, String faction, int factionIndex) {
            super(session); this.round = round; this.faction = faction; this.factionIndex = factionIndex;
        }

        public int round() { return round; }
        public String faction() { return faction; }
        public int factionIndex() { return factionIndex; }
    }

    public static final class PhaseStarted extends Event {
        private final int round;
        private final String faction;
        private final int factionIndex;

        public PhaseStarted(BattleSession session, int round, String faction, int factionIndex) {
            super(session); this.round = round; this.faction = faction; this.factionIndex = factionIndex;
        }

        public int round() { return round; }
        public String faction() { return faction; }
        public int factionIndex() { return factionIndex; }
    }

    public static final class PhaseEnding extends Event {
        private final int round;
        private final String faction;

        public PhaseEnding(BattleSession session, int round, String faction) {
            super(session); this.round = round; this.faction = faction;
        }

        public int round() { return round; }
        public String faction() { return faction; }
    }

    public static final class PhaseEnded extends Event {
        private final int round;
        private final String faction;

        public PhaseEnded(BattleSession session, int round, String faction) {
            super(session); this.round = round; this.faction = faction;
        }

        public int round() { return round; }
        public String faction() { return faction; }
    }

    public static final class MoveAboutToStart extends Cancellable {
        private final Combatant actor;
        private final BattleCell destination;
        private final int budget;

        public MoveAboutToStart(BattleSession session, Combatant actor, BattleCell destination, int budget) {
            super(session); this.actor = actor; this.destination = destination; this.budget = budget;
        }

        public Combatant actor() { return actor; }
        public BattleCell destination() { return destination; }
        public int budget() { return budget; }
    }

    public static final class MoveStarted extends Event {
        private final Combatant actor;
        private final BattleCell start;
        private final BattleCell destination;
        private final List<BattleCell> path;
        private final boolean consumesMovement;

        public MoveStarted(BattleSession session, Combatant actor, BattleCell start, BattleCell destination,
                           List<BattleCell> path, boolean consumesMovement) {
            super(session); this.actor = actor; this.start = start; this.destination = destination;
            this.path = path; this.consumesMovement = consumesMovement;
        }

        public Combatant actor() { return actor; }
        public BattleCell start() { return start; }
        public BattleCell destination() { return destination; }
        public List<BattleCell> path() { return path; }
        public boolean consumesMovement() { return consumesMovement; }
    }

    public static final class MoveCompleted extends Event {
        private final Combatant actor;
        private final BattleCell start;
        private final BattleCell destination;
        private final List<BattleCell> path;
        private final boolean consumesMovement;

        public MoveCompleted(BattleSession session, Combatant actor, BattleCell start, BattleCell destination,
                             List<BattleCell> path, boolean consumesMovement) {
            super(session); this.actor = actor; this.start = start; this.destination = destination;
            this.path = path; this.consumesMovement = consumesMovement;
        }

        public Combatant actor() { return actor; }
        public BattleCell start() { return start; }
        public BattleCell destination() { return destination; }
        public List<BattleCell> path() { return path; }
        public boolean consumesMovement() { return consumesMovement; }
    }

    public static final class Teleported extends Event {
        private final Combatant actor;
        private final BattleCell start;
        private final BattleCell destination;

        public Teleported(BattleSession session, Combatant actor, BattleCell start, BattleCell destination) {
            super(session); this.actor = actor; this.start = start; this.destination = destination;
        }

        public Combatant actor() { return actor; }
        public BattleCell start() { return start; }
        public BattleCell destination() { return destination; }
    }

    public static final class HealthSynchronized extends Event {
        private final Combatant combatant;
        private final float previousMaximum;
        private final float previousHealth;
        private final float maximum;
        private final float health;

        public HealthSynchronized(BattleSession session, Combatant combatant, float previousMaximum,
                                  float previousHealth, float maximum, float health) {
            super(session); this.combatant = combatant; this.previousMaximum = previousMaximum;
            this.previousHealth = previousHealth; this.maximum = maximum; this.health = health;
        }

        public Combatant combatant() { return combatant; }
        public float previousMaximum() { return previousMaximum; }
        public float previousHealth() { return previousHealth; }
        public float maximum() { return maximum; }
        public float health() { return health; }
    }

    public static final class ManaChanged extends Event {
        private final Combatant actor;
        private final float previous;
        private final float current;
        private final String causeId;

        public ManaChanged(BattleSession session, Combatant actor, float previous, float current, String causeId) {
            super(session); this.actor = actor; this.previous = previous; this.current = current; this.causeId = causeId;
        }

        public Combatant actor() { return actor; }
        public float previous() { return previous; }
        public float current() { return current; }
        public String causeId() { return causeId; }
    }

    public static final class BlockChanged extends Event {
        private final Combatant actor;
        private final float previous;
        private final float current;
        private final String causeId;

        public BlockChanged(BattleSession session, Combatant actor, float previous, float current, String causeId) {
            super(session); this.actor = actor; this.previous = previous; this.current = current; this.causeId = causeId;
        }

        public Combatant actor() { return actor; }
        public float previous() { return previous; }
        public float current() { return current; }
        public String causeId() { return causeId; }
    }

    public static final class StrengthChanged extends Event {
        private final Combatant actor;
        private final int previous;
        private final int current;
        private final String causeId;

        public StrengthChanged(BattleSession session, Combatant actor, int previous, int current, String causeId) {
            super(session); this.actor = actor; this.previous = previous; this.current = current; this.causeId = causeId;
        }

        public Combatant actor() { return actor; }
        public int previous() { return previous; }
        public int current() { return current; }
        public String causeId() { return causeId; }
    }

    public static final class CombatantSummoned extends Event {
        private final Combatant source;
        private final Combatant summoned;

        public CombatantSummoned(BattleSession session, Combatant source, Combatant summoned) {
            super(session); this.source = source; this.summoned = summoned;
        }

        public Combatant source() { return source; }
        public Combatant summoned() { return summoned; }
    }

    public static final class CombatantRemoved extends Event {
        private final Combatant combatant;
        private final String reason;

        public CombatantRemoved(BattleSession session, Combatant combatant, String reason) {
            super(session); this.combatant = combatant; this.reason = reason;
        }

        public Combatant combatant() { return combatant; }
        public String reason() { return reason; }
    }

    public static final class CardAboutToPlay extends Cancellable {
        private final Combatant actor;
        private final SkillCard card;
        private final CardDefinition cardDefinition;
        private final SkillDefinition definition;
        private final Combatant target;
        private final BattleCell targetCell;

        public CardAboutToPlay(BattleSession session, Combatant actor, SkillCard card, CardDefinition cardDefinition,
                               SkillDefinition definition, Combatant target, BattleCell targetCell) {
            super(session); this.actor = actor; this.card = card; this.cardDefinition = cardDefinition;
            this.definition = definition; this.target = target; this.targetCell = targetCell;
        }

        public Combatant actor() { return actor; }
        public SkillCard card() { return card; }
        public CardDefinition cardDefinition() { return cardDefinition; }
        public SkillDefinition definition() { return definition; }
        public Combatant target() { return target; }
        public BattleCell targetCell() { return targetCell; }
    }

    public static final class CardPlayed extends Event {
        private final Combatant actor;
        private final SkillCard card;
        private final SkillDefinition definition;
        private final Combatant target;
        private final BattleCell targetCell;
        private final double amount;
        private final boolean consumed;

        public CardPlayed(BattleSession session, Combatant actor, SkillCard card, SkillDefinition definition,
                          Combatant target, BattleCell targetCell, double amount, boolean consumed) {
            super(session); this.actor = actor; this.card = card; this.definition = definition; this.target = target;
            this.targetCell = targetCell; this.amount = amount; this.consumed = consumed;
        }

        public Combatant actor() { return actor; }
        public SkillCard card() { return card; }
        public SkillDefinition definition() { return definition; }
        public Combatant target() { return target; }
        public BattleCell targetCell() { return targetCell; }
        public double amount() { return amount; }
        public boolean consumed() { return consumed; }
    }

    public static final class SkillResolved extends Event {
        private final Combatant actor;
        private final SkillDefinition definition;
        private final Combatant target;
        private final double amount;

        public SkillResolved(BattleSession session, Combatant actor, SkillDefinition definition, Combatant target, double amount) {
            super(session); this.actor = actor; this.definition = definition; this.target = target; this.amount = amount;
        }

        public Combatant actor() { return actor; }
        public SkillDefinition definition() { return definition; }
        public Combatant target() { return target; }
        public double amount() { return amount; }
    }

    public static final class DamageAboutToBeDealt extends Cancellable {
        private final Combatant source;
        private final Combatant target;
        private final String causeId;
        private double amount;
        private final net.exmo.exworld.battle.combat.BattleDamageType damageType;
        private final java.util.Set<BattleCell> affectedCells;

        public DamageAboutToBeDealt(BattleSession session, Combatant source, Combatant target, String causeId, double amount) {
            this(session, source, target, causeId, amount, net.exmo.exworld.battle.combat.BattleDamageType.MAGIC, java.util.Set.of());
        }

        public DamageAboutToBeDealt(BattleSession session, Combatant source, Combatant target, String causeId, double amount,
                                    net.exmo.exworld.battle.combat.BattleDamageType damageType,
                                    java.util.Collection<BattleCell> affectedCells) {
            super(session); this.source = source; this.target = target; this.causeId = causeId; this.amount = Math.max(0, amount);
            this.damageType = damageType == null ? net.exmo.exworld.battle.combat.BattleDamageType.MAGIC : damageType;
            this.affectedCells = affectedCells == null ? java.util.Set.of() : java.util.Set.copyOf(affectedCells);
        }

        public Combatant source() { return source; }
        public Combatant target() { return target; }
        public String causeId() { return causeId; }
        public double amount() { return amount; }
        public void amount(double value) { amount = Math.max(0, value); }
        public net.exmo.exworld.battle.combat.BattleDamageType damageType() { return damageType; }
        public java.util.Set<BattleCell> affectedCells() { return affectedCells; }
    }

    public static final class DamageDealt extends Event {
        private final Combatant source;
        private final Combatant target;
        private final String causeId;
        private final double requestedAmount;
        private final double amount;

        public DamageDealt(BattleSession session, Combatant source, Combatant target, String causeId,
                           double requestedAmount, double amount) {
            super(session); this.source = source; this.target = target; this.causeId = causeId;
            this.requestedAmount = requestedAmount; this.amount = amount;
        }

        public Combatant source() { return source; }
        public Combatant target() { return target; }
        public String causeId() { return causeId; }
        public double requestedAmount() { return requestedAmount; }
        public double amount() { return amount; }
    }

    public static final class HealingAboutToBeApplied extends Cancellable {
        private final Combatant source;
        private final Combatant target;
        private final String causeId;
        private double amount;

        public HealingAboutToBeApplied(BattleSession session, Combatant source, Combatant target, String causeId, double amount) {
            super(session); this.source = source; this.target = target; this.causeId = causeId; this.amount = Math.max(0, amount);
        }

        public Combatant source() { return source; }
        public Combatant target() { return target; }
        public String causeId() { return causeId; }
        public double amount() { return amount; }
        public void amount(double value) { amount = Math.max(0, value); }
    }

    public static final class Healed extends Event {
        private final Combatant source;
        private final Combatant target;
        private final String causeId;
        private final double requestedAmount;
        private final double amount;

        public Healed(BattleSession session, Combatant source, Combatant target, String causeId,
                      double requestedAmount, double amount) {
            super(session); this.source = source; this.target = target; this.causeId = causeId;
            this.requestedAmount = requestedAmount; this.amount = amount;
        }

        public Combatant source() { return source; }
        public Combatant target() { return target; }
        public String causeId() { return causeId; }
        public double requestedAmount() { return requestedAmount; }
        public double amount() { return amount; }
    }

    public static final class StatusAboutToBeApplied extends Cancellable {
        private final Combatant source;
        private final Combatant target;
        private BattleStatus status;

        public StatusAboutToBeApplied(BattleSession session, Combatant source, Combatant target, BattleStatus status) {
            super(session); this.source = source; this.target = target; this.status = status;
        }

        public Combatant source() { return source; }
        public Combatant target() { return target; }
        public BattleStatus status() { return status; }
        public void status(BattleStatus value) { if (value != null) status = value; }
    }

    public static final class StatusApplied extends Event {
        private final Combatant source;
        private final Combatant target;
        private final BattleStatus status;

        public StatusApplied(BattleSession session, Combatant source, Combatant target, BattleStatus status) {
            super(session); this.source = source; this.target = target; this.status = status;
        }

        public Combatant source() { return source; }
        public Combatant target() { return target; }
        public BattleStatus status() { return status; }
    }

    public static final class CombatantDowned extends Event {
        private final Combatant source;
        private final Combatant target;
        private final String causeId;

        public CombatantDowned(BattleSession session, Combatant source, Combatant target, String causeId) {
            super(session); this.source = source; this.target = target; this.causeId = causeId;
        }

        public Combatant source() { return source; }
        public Combatant target() { return target; }
        public String causeId() { return causeId; }
    }

    public static final class ItemAboutToBeUsed extends Cancellable {
        private final Combatant actor;
        private final int inventorySlot;
        private final String itemId;
        private final BattleCell targetCell;

        public ItemAboutToBeUsed(BattleSession session, Combatant actor, int inventorySlot, String itemId, BattleCell targetCell) {
            super(session); this.actor = actor; this.inventorySlot = inventorySlot; this.itemId = itemId; this.targetCell = targetCell;
        }

        public Combatant actor() { return actor; }
        public int inventorySlot() { return inventorySlot; }
        public String itemId() { return itemId; }
        public BattleCell targetCell() { return targetCell; }
    }

    public static final class ItemUsed extends Event {
        private final Combatant actor;
        private final int inventorySlot;
        private final String itemId;
        private final BattleCell targetCell;

        public ItemUsed(BattleSession session, Combatant actor, int inventorySlot, String itemId, BattleCell targetCell) {
            super(session); this.actor = actor; this.inventorySlot = inventorySlot; this.itemId = itemId; this.targetCell = targetCell;
        }

        public Combatant actor() { return actor; }
        public int inventorySlot() { return inventorySlot; }
        public String itemId() { return itemId; }
        public BattleCell targetCell() { return targetCell; }
    }

    public static final class ItemUseRejected extends Event {
        private final Combatant actor;
        private final int inventorySlot;
        private final String itemId;
        private final String reason;

        public ItemUseRejected(BattleSession session, Combatant actor, int inventorySlot, String itemId, String reason) {
            super(session); this.actor = actor; this.inventorySlot = inventorySlot; this.itemId = itemId; this.reason = reason;
        }

        public Combatant actor() { return actor; }
        public int inventorySlot() { return inventorySlot; }
        public String itemId() { return itemId; }
        public String reason() { return reason; }
    }

    public static final class WeaponSwitchAboutToStart extends Cancellable {
        private final Combatant actor;
        private final int weaponSlot;

        public WeaponSwitchAboutToStart(BattleSession session, Combatant actor, int weaponSlot) {
            super(session); this.actor = actor; this.weaponSlot = weaponSlot;
        }

        public Combatant actor() { return actor; }
        public int weaponSlot() { return weaponSlot; }
    }

    public static final class WeaponSwitched extends Event {
        private final Combatant actor;
        private final int previousSlot;
        private final int currentSlot;
        private final String itemId;

        public WeaponSwitched(BattleSession session, Combatant actor, int previousSlot, int currentSlot, String itemId) {
            super(session); this.actor = actor; this.previousSlot = previousSlot; this.currentSlot = currentSlot; this.itemId = itemId;
        }

        public Combatant actor() { return actor; }
        public int previousSlot() { return previousSlot; }
        public int currentSlot() { return currentSlot; }
        public String itemId() { return itemId; }
    }

    public static final class PassiveTriggered extends Event {
        private final Combatant actor;
        private final String passiveId;
        private final String trigger;
        private final int progress;

        public PassiveTriggered(BattleSession session, Combatant actor, String passiveId, String trigger, int progress) {
            super(session); this.actor = actor; this.passiveId = passiveId; this.trigger = trigger; this.progress = progress;
        }

        public Combatant actor() { return actor; }
        public String passiveId() { return passiveId; }
        public String trigger() { return trigger; }
        public int progress() { return progress; }
    }

    public static final class PassiveRewardGranted extends Event {
        private final Combatant actor;
        private final String passiveId;
        private final String rewardId;

        public PassiveRewardGranted(BattleSession session, Combatant actor, String passiveId, String rewardId) {
            super(session); this.actor = actor; this.passiveId = passiveId; this.rewardId = rewardId;
        }

        public Combatant actor() { return actor; }
        public String passiveId() { return passiveId; }
        public String rewardId() { return rewardId; }
    }

    public static final class Presentation extends Event {
        private final BattleEvent event;

        public Presentation(BattleSession session, BattleEvent event) { super(session); this.event = event; }
        public BattleEvent event() { return event; }
    }
}
