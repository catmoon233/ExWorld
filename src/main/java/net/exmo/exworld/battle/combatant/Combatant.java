package net.exmo.exworld.battle.combatant;

import net.exmo.exworld.battle.api.EncounterRequest;
import net.exmo.exworld.battle.card.DeckState;
import net.exmo.exworld.battle.model.BattleCell;
import net.exmo.exworld.battle.model.BattleStatus;
import net.exmo.exworld.battle.model.CombatantAttribute;
import net.exmo.exworld.battle.weapon.WeaponFamily;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.UUID;

/** Battle-local role snapshot. Persistent character growth remains outside this aggregate. */
public final class Combatant {
    private final UUID id;
    private final UUID playerId;
    private final String name;
    private final String factionId;
    private BattleCell cell;
    private float maxHealth;
    private float health;
    private final float maxMana;
    private float mana;
    private final float manaPerPhase;
    private final double initiative;
    private final int movementPoints;
    private final int actionPoints;
    private boolean unlimitedActionPoints;
    private final int drawPerPhase;
    private int actionPointsRemaining;
    private int movementRemaining;
    private boolean downed;
    private boolean autoBattle;
    private boolean basicAttackAvailable = true;
    private final DeckState deck;
    private final Map<String, BattleStatus> statuses = new LinkedHashMap<>();
    private final Set<String> usedPowers = new LinkedHashSet<>();
    private final Set<String> permanentCardAdditions = new LinkedHashSet<>();
    private float block;
    private int strength;
    private int itemUseLimit = 1;
    private int itemUsesRemaining = 1;
    private int activeWeaponSlot;
    private String weaponSlot1 = "";
    private String weaponSlot2 = "";
    private int attackCardCount;
    private final Map<String, Integer> passiveCounters = new LinkedHashMap<>();
    private final Set<CombatantAttribute> attributes;
    private int strengthLevel = 1;
    private double weaponAttack;
    private WeaponFamily weaponFamily = WeaponFamily.NONE;
    private List<String> modifierEntryIds = List.of();
    private List<String> modifierAttributes = List.of();
    private List<String> elementIds = List.of();
    private int dodgeDx;
    private int dodgeDz;
    private int interceptDx;
    private int interceptDz;

    public Combatant(EncounterRequest.CombatantSeed seed, long battleSeed) {
        id = seed.entityId(); playerId = seed.playerId(); name = seed.name(); factionId = seed.factionId(); cell = seed.cell();
        maxHealth = Math.max(1, seed.maxHealth()); health = Math.min(maxHealth, Math.max(0, seed.health()));
        maxMana = Math.max(0, seed.maxMana()); mana = Math.min(maxMana, Math.max(0, seed.mana()));
        manaPerPhase = Math.max(0, seed.manaPerPhase()); initiative = seed.initiative(); movementPoints = Math.max(0, seed.movementPoints()); movementRemaining = movementPoints;
        // Legacy encounter callers did not carry an AI budget; retain the documented hostile default.
        // Negative action points on a player mean an unlimited budget so existing tests stay intact.
        unlimitedActionPoints = seed.actionPoints() < 0 && seed.playerControlled();
        actionPoints = seed.actionPoints() < 0 ? (seed.playerControlled() ? 0 : 3) : seed.actionPoints(); drawPerPhase = seed.drawPerPhase(); actionPointsRemaining = unlimitedActionPoints ? 0 : actionPoints;
        downed = health <= 0; attributes = seed.attributes(); deck = new DeckState(seed.deck(), seed.deckStars(), battleSeed ^ id.getMostSignificantBits()); deck.drawInitial(seed.initialHandSize());
    }

    public Combatant(UUID id, UUID playerId, String name, String factionId, BattleCell cell,
                     float maxHealth, float health, float maxMana, float mana, float manaPerPhase, double initiative,
                     int movementPoints, int movementRemaining, boolean downed, boolean autoBattle,
                     boolean basicAttackAvailable, DeckState deck) {
        this(id, playerId, name, factionId, cell, maxHealth, health, maxMana, mana, manaPerPhase, initiative,
                movementPoints, movementRemaining, downed, autoBattle, basicAttackAvailable, deck, 0, DeckState.DRAW_PER_PHASE, 0);
    }

    public Combatant(UUID id, UUID playerId, String name, String factionId, BattleCell cell,
                     float maxHealth, float health, float maxMana, float mana, float manaPerPhase, double initiative,
                     int movementPoints, int movementRemaining, boolean downed, boolean autoBattle,
                     boolean basicAttackAvailable, DeckState deck, int actionPoints) {
        this(id, playerId, name, factionId, cell, maxHealth, health, maxMana, mana, manaPerPhase, initiative, movementPoints, movementRemaining, downed, autoBattle, basicAttackAvailable, deck, actionPoints, DeckState.DRAW_PER_PHASE, actionPoints);
    }
    public Combatant(UUID id, UUID playerId, String name, String factionId, BattleCell cell,
                     float maxHealth, float health, float maxMana, float mana, float manaPerPhase, double initiative,
                     int movementPoints, int movementRemaining, boolean downed, boolean autoBattle,
                     boolean basicAttackAvailable, DeckState deck, int actionPoints, int drawPerPhase, int actionPointsRemaining) {
        this(id, playerId, name, factionId, cell, maxHealth, health, maxMana, mana, manaPerPhase, initiative,
                movementPoints, movementRemaining, downed, autoBattle, basicAttackAvailable, deck, actionPoints, drawPerPhase, actionPointsRemaining, Set.of());
    }
    public Combatant(UUID id, UUID playerId, String name, String factionId, BattleCell cell,
                     float maxHealth, float health, float maxMana, float mana, float manaPerPhase, double initiative,
                     int movementPoints, int movementRemaining, boolean downed, boolean autoBattle,
                     boolean basicAttackAvailable, DeckState deck, int actionPoints, int drawPerPhase, int actionPointsRemaining,
                     Set<CombatantAttribute> attributes) {
        this.id = id; this.playerId = playerId; this.name = name; this.factionId = factionId; this.cell = cell;
        this.maxHealth = maxHealth; this.health = health; this.maxMana = maxMana; this.mana = mana; this.manaPerPhase = manaPerPhase;
        this.initiative = initiative; this.movementPoints = movementPoints; this.movementRemaining = movementRemaining;
        this.actionPoints = Math.max(0, actionPoints); this.drawPerPhase=Math.max(0,drawPerPhase);this.actionPointsRemaining=Math.max(0,Math.min(this.actionPoints,actionPointsRemaining));
        this.unlimitedActionPoints = false;
        this.downed = downed; this.autoBattle = autoBattle; this.basicAttackAvailable = basicAttackAvailable; this.deck = deck;
        this.attributes = attributes == null ? Set.of() : Set.copyOf(attributes);
    }

    public UUID id() { return id; }
    public UUID playerId() { return playerId; }
    public String name() { return name; }
    public String factionId() { return factionId; }
    public BattleCell cell() { return cell; }
    public float maxHealth() { return maxHealth; }
    public float health() { return health; }
    public float maxMana() { return maxMana; }
    public float mana() { return mana; }
    public float manaPerPhase() { return manaPerPhase; }
    public double initiative() { return initiative; }
    public int movementRemaining() { return movementRemaining; }
    public int movementPoints() { return movementPoints; }
    public int actionPoints() { return actionPoints; }
    public int actionPointsRemaining() { return actionPointsRemaining; }
    public boolean unlimitedActionPoints() { return unlimitedActionPoints; }
    public void setUnlimitedActionPoints(boolean value) { unlimitedActionPoints = value; }
    public int drawPerPhase() { return drawPerPhase; }
    public boolean hasActionPoint() { return unlimitedActionPoints || actionPointsRemaining > 0; }
    public boolean downed() { return downed; }
    public boolean autoBattle() { return autoBattle; }
    public boolean playerControlled() { return playerId != null; }
    public DeckState deck() { return deck; }
    public boolean basicAttackAvailable() { return basicAttackAvailable; }
    public List<BattleStatus> statuses() { return List.copyOf(statuses.values()); }
    public float block() { return block; }
    public int strength() { return strength; }
    public int itemUseLimit() { return itemUseLimit; }
    public int itemUsesRemaining() { return itemUsesRemaining; }
    public int activeWeaponSlot() { return activeWeaponSlot; }
    public String weaponItem(int slot) { return slot == 1 ? weaponSlot1 : slot == 2 ? weaponSlot2 : ""; }
    public String activeWeaponItem() { return weaponItem(activeWeaponSlot); }
    public int attackCardCount() { return attackCardCount; }
    public int passiveCounter(String id) { return passiveCounters.getOrDefault(id, 0); }
    public Set<CombatantAttribute> attributes() { return attributes; }
    public boolean hasAttribute(CombatantAttribute attribute) { return attributes.contains(attribute); }
    public double elevation() { return attributes.stream().mapToDouble(CombatantAttribute::elevation).max().orElse(0.0D); }
    public boolean blocksRangedLineOfSight() { return attributes.stream().allMatch(CombatantAttribute::blocksRangedLineOfSight); }
    public Map<String, Integer> passiveCounters() { return Map.copyOf(passiveCounters); }
    public void configureEquipment(String first, String second, int activeSlot) {
        weaponSlot1 = first == null ? "" : first; weaponSlot2 = second == null ? "" : second;
        activeWeaponSlot = activeSlot >= 1 && activeSlot <= 2 ? activeSlot : 0;
        if (activeWeaponItem().isBlank()) activeWeaponSlot = 0;
        weaponFamily = WeaponFamily.of(activeWeaponItem());
    }
    public boolean setActiveWeaponSlot(int slot) {
        int normalized = slot >= 1 && slot <= 2 ? slot : 0;
        if (activeWeaponSlot == normalized) return false;
        activeWeaponSlot = normalized;
        weaponFamily = WeaponFamily.of(activeWeaponItem());
        return true;
    }
    public void setItemUseLimit(int limit) { itemUseLimit = Math.max(0, limit); itemUsesRemaining = Math.min(itemUseLimit, Math.max(0, itemUsesRemaining)); }
    public boolean consumeItemUse() { if (itemUsesRemaining <= 0) return false; itemUsesRemaining--; return true; }
    public void restoreItemUses() { itemUsesRemaining = itemUseLimit; }
    public void setItemUsesRemaining(int value) { itemUsesRemaining = Math.max(0, Math.min(itemUseLimit, value)); }
    public void addAttackCardCount() { attackCardCount++; }
    public void setAttackCardCount(int count) { attackCardCount = Math.max(0, count); }
    public void setPassiveCounter(String id, int value) { if (id != null && !id.isBlank()) passiveCounters.put(id, Math.max(0, value)); }
    public int incrementPassiveCounter(String id) { int next = passiveCounter(id) + 1; setPassiveCounter(id, next); return next; }
    public void restorePassiveCounters(Map<String, Integer> values) { passiveCounters.clear(); values.forEach(this::setPassiveCounter); }
    public boolean powerUsed(String cardId) { return usedPowers.contains(cardId); }
    public void markPowerUsed(String cardId) { if (cardId != null) usedPowers.add(cardId); }
    public void restoreUsedPowers(Collection<String> powers) { usedPowers.clear(); usedPowers.addAll(powers); }
    public Set<String> usedPowers() { return Set.copyOf(usedPowers); }
    public void addPermanentCard(String cardId) { if (cardId != null && !cardId.isBlank()) permanentCardAdditions.add(cardId); }
    public Set<String> permanentCardAdditions() { return Set.copyOf(permanentCardAdditions); }
    public void restorePermanentCardAdditions(Collection<String> cards) { permanentCardAdditions.clear(); permanentCardAdditions.addAll(cards); }
    public void addBlock(float amount) { block = Math.max(0, block + Math.max(0, amount)); }
    public void addStrength(int amount) { strength = Math.max(0, Math.min(99, strength + amount)); }
    public int strengthLevel() { return strengthLevel; }
    public void setStrengthLevel(int value) { strengthLevel = Math.max(1, Math.min(99, value)); }
    public double weaponAttack() { return weaponAttack; }
    public void setWeaponAttack(double value) { weaponAttack = Math.max(0, value); }
    public WeaponFamily weaponFamily() { return weaponFamily; }
    public void setWeaponFamily(WeaponFamily family) { weaponFamily = family == null ? WeaponFamily.NONE : family; }
    public void setModifierSnapshot(List<String> entryIds, List<String> attributes) {
        modifierEntryIds = entryIds == null ? List.of() : List.copyOf(entryIds);
        modifierAttributes = attributes == null ? List.of() : List.copyOf(attributes);
    }
    public List<String> modifierEntryIds() { return modifierEntryIds; }
    public List<String> modifierAttributes() { return modifierAttributes; }
    public void setElementSnapshot(List<String> ids) { elementIds = ids == null ? List.of() : List.copyOf(ids); }
    public List<String> elementIds() { return elementIds; }
    public int dodgeDx() { return dodgeDx; }
    public int dodgeDz() { return dodgeDz; }
    public void setDodgeDirection(int dx, int dz) { dodgeDx = Integer.signum(dx); dodgeDz = Integer.signum(dz); }
    public int interceptDx() { return interceptDx; }
    public int interceptDz() { return interceptDz; }
    public void setInterceptDirection(int dx, int dz) { interceptDx = Integer.signum(dx); interceptDz = Integer.signum(dz); }
    public Optional<BattleStatus> status(String id) { return Optional.ofNullable(statuses.get(id)); }
    public void setAutoBattle(boolean value) { autoBattle = value; }
    public void moveTo(BattleCell destination, int cost) { cell = destination; movementRemaining = Math.max(0, movementRemaining - cost); }
    public boolean consumeActionPoint() {
        if (unlimitedActionPoints) return true;
        if (actionPointsRemaining <= 0) return false;
        actionPointsRemaining--;
        return true;
    }
    public int spendAllActionPoints() {
        if (unlimitedActionPoints) return 1;
        int spent = actionPointsRemaining;
        actionPointsRemaining = 0;
        return spent;
    }
    public void relocate(BattleCell destination) { cell = destination; }
    public void spendMana(float cost) { mana = Math.max(0, mana - cost); }
    public void restoreMana(float amount) { mana = Math.min(maxMana, mana + Math.max(0, amount)); }
    public void damage(float amount) {
        if (downed) return;
        float incoming = Math.max(0, amount) * incomingDamageMultiplier();
        float absorbed = Math.min(block, incoming); block -= absorbed; incoming -= absorbed;
        health = Math.max(0, health - incoming);
        if (health <= 0) downed = true;
    }
    public float incomingDamageMultiplier() { return statuses.containsKey("exworld:guarded") ? .75F : statuses.containsKey("exworld:iron_shield") ? .65F : 1.0F; }
    public void syncHealth(float maximum, float current) { maxHealth=Math.max(1,maximum);health=Math.min(maxHealth,Math.max(0,current));downed=health<=0; }
    public void heal(float amount) { health = Math.min(maxHealth, health + Math.max(0, amount)); if (health > 0) downed = false; }
    public void addStatus(BattleStatus status) { statuses.merge(status.id(), status, (old, next) -> old.merge(next.stacks(), next.remainingRounds())); }
    public void replaceStatus(BattleStatus status) { statuses.put(status.id(), status); }
    public void removeStatus(String id) { statuses.remove(id); }
    public void restoreStatuses(Collection<BattleStatus> restored) { statuses.clear(); restored.forEach(status -> statuses.put(status.id(), status)); }
    public void consumeBasicAttack() { basicAttackAvailable = false; }
    public void beginPhase() {
        boolean slowed = statuses.containsKey("exworld:iron_slow") || statuses.containsKey("minecraft:slowness");
        Iterator<Map.Entry<String, BattleStatus>> iterator = statuses.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, BattleStatus> entry = iterator.next();
            BattleStatus status = entry.getValue();
            if (status.id().equals("exworld:taunt")) {
                if (status.stacks() <= 1) { iterator.remove(); continue; }
                entry.setValue(new BattleStatus(status.id(), status.nameKey(), status.stacks() - 1, status.remainingRounds(),
                        status.beneficial(), status.permanent(), status.potion(), status.preservesPotionLevel()));
                continue;
            }
            if (status.potion() && !status.preservesPotionLevel()) {
                if (status.stacks() <= 1) { iterator.remove(); continue; }
                status = status.lowerPotionLevel();
            }
            if (!status.permanent() && status.remainingRounds() <= 1) { iterator.remove(); continue; }
            entry.setValue(status.tick());
        }
        block = 0;
        movementRemaining = slowed ? Math.max(1, movementPoints / 2) : movementPoints;
        basicAttackAvailable = true; actionPointsRemaining = unlimitedActionPoints ? 0 : actionPoints; restoreItemUses(); deck.drawForPhase(drawPerPhase); restoreMana(manaPerPhase);
    }
}
