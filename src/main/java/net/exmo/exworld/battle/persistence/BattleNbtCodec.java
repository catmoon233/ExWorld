package net.exmo.exworld.battle.persistence;

import net.exmo.exworld.battle.BattleSession;
import net.exmo.exworld.battle.action.BattleActionTimeline;
import net.exmo.exworld.battle.api.*;
import net.exmo.exworld.battle.arena.ArenaDefinition;
import net.exmo.exworld.battle.arena.ArenaGrid;
import net.exmo.exworld.battle.card.DeckState;
import net.exmo.exworld.battle.card.SkillCard;
import net.exmo.exworld.battle.combatant.Combatant;
import net.exmo.exworld.battle.combat.BattleEffectResolver;
import net.exmo.exworld.battle.model.*;
import net.exmo.exworld.battle.skill.SkillRegistry;
import net.minecraft.nbt.*;

import java.util.*;

public final class BattleNbtCodec {
    private BattleNbtCodec() {}
    public static CompoundTag save(BattleSession session) {
        BattleSnapshot snapshot = session.snapshot(); CompoundTag tag = new CompoundTag();
        tag.putUUID("id", snapshot.battleId().value()); tag.putString("encounter", session.request().encounterId()); tag.putString("arena", snapshot.arenaId());
        if (session.request().openingFaction() != null) tag.putString("opening_faction", session.request().openingFaction());
        if (session.request().rewardPool() != null) tag.putString("reward_pool", session.request().rewardPool());
        if (session.request().introProfile() != null) tag.putString("intro_profile", session.request().introProfile());
        tag.putLong("seed", session.request().seed()); tag.putString("state", snapshot.state().name()); tag.putLong("revision", snapshot.revision());
        tag.putString("host_dimension", session.request().host().dimension()); tag.putBoolean("host_owns_arena", session.request().host().ownsArena());
        tag.putInt("host_floor_y", session.request().host().floorY());
        if (session.request().host().dungeon()) { tag.putUUID("dungeon_run", session.request().host().dungeonRunId()); tag.putString("dungeon_room", session.request().host().roomId()); }
        tag.putLong("event_sequence", snapshot.eventSequence()); tag.putInt("origin_x", snapshot.arenaOriginX()); tag.putInt("origin_z", snapshot.arenaOriginZ());
        tag.putInt("round", snapshot.round()); tag.putInt("faction_index", session.factionIndex()); tag.putInt("ticks", snapshot.phaseTicksRemaining());
        tag.putLong("battle_ticks", session.battleTicks());
        ListTag order = new ListTag(); snapshot.factionOrder().forEach(value -> order.add(StringTag.valueOf(value))); tag.put("faction_order", order);
        ListTag ready = new ListTag(); snapshot.readyPlayers().forEach(id -> { CompoundTag value = new CompoundTag(); value.putUUID("id", id); ready.add(value); }); tag.put("ready", ready);
        ListTag skipped = new ListTag(); session.introSkippedPlayers().forEach(id -> { CompoundTag value = new CompoundTag(); value.putUUID("id", id); skipped.add(value); }); tag.put("intro_skipped", skipped);
        tag.putDouble("phase_damage", session.phaseDamage());
        ListTag motions = new ListTag(); session.actions().moves().forEach(move -> {
            CompoundTag value = new CompoundTag(); value.putUUID("actor", move.actorId()); value.putInt("duration", move.durationTicks()); value.putInt("elapsed", move.elapsedTicks()); value.putBoolean("consumes_movement", move.consumesMovement());
            CompoundTag start = new CompoundTag(); saveCell(start, move.start()); value.put("start", start);
            ListTag path = new ListTag(); move.path().forEach(cell -> { CompoundTag point = new CompoundTag(); saveCell(point, cell); path.add(point); }); value.put("path", path); motions.add(value);
        }); tag.put("motions", motions);
        ListTag actionLocks = new ListTag(); session.actionLocks().forEach((actor,ticks) -> {
            CompoundTag value=new CompoundTag();value.putUUID("actor",actor);value.putInt("ticks",ticks);actionLocks.add(value);
        });tag.put("action_locks",actionLocks);
        ListTag pendingDashes = new ListTag(); session.pendingDashes().forEach((actor, dash) -> {
            CompoundTag value = new CompoundTag(); value.putUUID("actor", actor);
            ListTag targets = new ListTag(); dash.targetIds().forEach(id -> { CompoundTag target = new CompoundTag(); target.putUUID("id", id); targets.add(target); });
            value.put("targets", targets); pendingDashes.add(value);
        }); tag.put("pending_dashes", pendingDashes);
        session.pendingResult().ifPresent(result -> { tag.putString("outcome", result.outcome().name()); });
        ListTag rewards = new ListTag(); session.persistedRewards().forEach((playerId, reward) -> {
            CompoundTag value = new CompoundTag(); value.putUUID("player", playerId); value.putBoolean("confirmed", reward.confirmed()); value.putInt("gold", reward.gold());
            if (reward.selected() != null) value.putString("selected", reward.selected());
            ListTag candidates = new ListTag(); reward.candidates().forEach(candidate -> candidates.add(StringTag.valueOf(candidate))); value.put("candidates", candidates); rewards.add(value);
        }); tag.put("rewards", rewards);
        ListTag statistics = new ListTag(); session.persistedStatistics().forEach((playerId, stat) -> {
            CompoundTag value = new CompoundTag(); value.putUUID("player", playerId); value.putDouble("dealt", stat.damageDealt()); value.putDouble("taken", stat.damageTaken()); value.putDouble("healing", stat.healing()); value.putInt("cards", stat.cardsUsed()); statistics.add(value);
        }); tag.put("statistics", statistics);
        ListTag events = new ListTag(); snapshot.events().forEach(event -> { CompoundTag value = new CompoundTag(); value.putLong("sequence", event.sequence()); value.putInt("round", event.round()); value.putString("type", event.type().name()); value.putUUID("actor", event.actorId()); if (event.targetId() != null) value.putUUID("target", event.targetId()); value.putString("actor_name", event.actorName()); value.putString("target_name", event.targetName()); value.putString("skill", event.skillId()); value.putString("skill_name", event.skillNameKey()); value.putDouble("amount", event.amount()); if (event.fromCell() != null) { CompoundTag cell = new CompoundTag(); saveCell(cell, event.fromCell()); value.put("from", cell); } if (event.toCell() != null) { CompoundTag cell = new CompoundTag(); saveCell(cell, event.toCell()); value.put("to", cell); } events.add(value); }); tag.put("events", events);
        ListTag combatants = new ListTag(); session.combatants().forEach(combatant -> combatants.add(saveCombatant(combatant))); tag.put("combatants", combatants);
        ListTag relations = new ListTag(); session.request().relations().forEach((pair, relation) -> { CompoundTag value = new CompoundTag(); value.putString("from", pair.from()); value.putString("to", pair.to()); value.putString("relation", relation.name()); relations.add(value); }); tag.put("relations", relations);
        ListTag returns = new ListTag(); session.request().returnPoints().forEach((id, point) -> { CompoundTag value = new CompoundTag(); value.putUUID("player", id); value.putString("dimension", point.dimension()); value.putDouble("x", point.x()); value.putDouble("y", point.y()); value.putDouble("z", point.z()); value.putFloat("yaw", point.yaw()); value.putFloat("pitch", point.pitch()); returns.add(value); }); tag.put("returns", returns);
        CompoundTag variables = new CompoundTag(); session.request().variables().forEach(variables::putString); tag.put("variables", variables);
        return tag;
    }

    public static BattleSession load(CompoundTag tag, ArenaDefinition arena, SkillRegistry skills) {
        return load(tag,arena,skills,BattleEffectResolver.logical());
    }
    public static BattleSession load(CompoundTag tag, ArenaDefinition arena, SkillRegistry skills, BattleEffectResolver effects) {
        Map<EncounterRequest.FactionPair, FactionRelation> relations = new LinkedHashMap<>();
        ListTag relationTags = tag.getList("relations", Tag.TAG_COMPOUND); for (int i = 0; i < relationTags.size(); i++) { CompoundTag value = relationTags.getCompound(i); relations.put(new EncounterRequest.FactionPair(value.getString("from"), value.getString("to")), FactionRelation.valueOf(value.getString("relation"))); }
        Map<UUID, EncounterRequest.ReturnPoint> returns = new LinkedHashMap<>();
        ListTag returnTags = tag.getList("returns", Tag.TAG_COMPOUND); for (int i = 0; i < returnTags.size(); i++) { CompoundTag value = returnTags.getCompound(i); returns.put(value.getUUID("player"), new EncounterRequest.ReturnPoint(value.getString("dimension"), value.getDouble("x"), value.getDouble("y"), value.getDouble("z"), value.getFloat("yaw"), value.getFloat("pitch"))); }
        List<Combatant> combatants = new ArrayList<>(); List<EncounterRequest.CombatantSeed> seeds = new ArrayList<>();
        ListTag combatantTags = tag.getList("combatants", Tag.TAG_COMPOUND); for (int i = 0; i < combatantTags.size(); i++) { CompoundTag value = combatantTags.getCompound(i); Combatant combatant = loadCombatant(value, tag.getLong("seed")); combatants.add(combatant); seeds.add(new EncounterRequest.CombatantSeed(combatant.id(), combatant.playerId(), combatant.name(), combatant.factionId(), combatant.cell(), combatant.maxHealth(), combatant.health(), combatant.maxMana(), combatant.mana(), combatant.manaPerPhase(), combatant.initiative(), combatant.movementPoints(), List.of(), value.contains("action_points", Tag.TAG_INT) ? value.getInt("action_points") : 0)); }
        String openingFaction = tag.contains("opening_faction", Tag.TAG_STRING) ? tag.getString("opening_faction") : null;
        String hostDimension = tag.contains("host_dimension", Tag.TAG_STRING) ? tag.getString("host_dimension") : "exworld:battle";
        BattleHost host = tag.hasUUID("dungeon_run")
                ? BattleHost.dungeon(hostDimension, tag.getInt("origin_x"), tag.getInt("origin_z"), tag.getInt("host_floor_y"), tag.getUUID("dungeon_run"), tag.getString("dungeon_room"))
                : new BattleHost(hostDimension, tag.getInt("origin_x"), tag.getInt("origin_z"), tag.contains("host_floor_y", Tag.TAG_INT) ? tag.getInt("host_floor_y") : 64, !tag.contains("host_owns_arena", Tag.TAG_BYTE) || tag.getBoolean("host_owns_arena"), null, "");
        Map<String, String> variables = new LinkedHashMap<>();
        if (tag.contains("variables", Tag.TAG_COMPOUND)) {
            CompoundTag values = tag.getCompound("variables");
            values.getAllKeys().forEach(key -> variables.put(key, values.getString(key)));
        }
        EncounterRequest request = new EncounterRequest(tag.getString("encounter"), tag.getString("arena"), seeds, relations, returns, tag.getLong("seed"), variables, openingFaction,
                tag.contains("reward_pool",Tag.TAG_STRING)?tag.getString("reward_pool"):null, tag.contains("intro_profile",Tag.TAG_STRING)?tag.getString("intro_profile"):null, host);
        List<String> order = new ArrayList<>(); ListTag orderTags = tag.getList("faction_order", Tag.TAG_STRING); for (int i = 0; i < orderTags.size(); i++) order.add(orderTags.getString(i));
        Set<UUID> ready = new LinkedHashSet<>(); ListTag readyTags = tag.getList("ready", Tag.TAG_COMPOUND); for (int i = 0; i < readyTags.size(); i++) ready.add(readyTags.getCompound(i).getUUID("id"));
        BattleSession session = new BattleSession(new BattleId(tag.getUUID("id")), request, new ArenaGrid(arena), skills, tag.getInt("origin_x"), tag.getInt("origin_z"),
                combatants, order, BattleState.valueOf(tag.getString("state")), tag.getLong("revision"), tag.getLong("event_sequence"),
                tag.getInt("round"), tag.getInt("faction_index"), tag.getInt("ticks"), ready, effects);
        List<BattleEvent> events = new ArrayList<>(); ListTag eventTags = tag.getList("events", Tag.TAG_COMPOUND); for (int i = 0; i < eventTags.size(); i++) { CompoundTag value = eventTags.getCompound(i); BattleCell from = value.contains("from", Tag.TAG_COMPOUND) ? loadCell(value.getCompound("from")) : null; BattleCell to = value.contains("to", Tag.TAG_COMPOUND) ? loadCell(value.getCompound("to")) : null; events.add(new BattleEvent(value.getLong("sequence"), value.getInt("round"), BattleEvent.Type.valueOf(value.getString("type")), value.getUUID("actor"), value.hasUUID("target") ? value.getUUID("target") : null, value.getString("actor_name"), value.getString("target_name"), value.getString("skill"), value.getString("skill_name"), value.getDouble("amount"), from, to)); } session.restoreEvents(events);
        Set<UUID> skipped = new LinkedHashSet<>(); ListTag skipTags = tag.getList("intro_skipped", Tag.TAG_COMPOUND); for (int i=0;i<skipTags.size();i++) skipped.add(skipTags.getCompound(i).getUUID("id"));
        List<BattleActionTimeline.MoveAction> motions = new ArrayList<>(); ListTag motionTags = tag.getList("motions", Tag.TAG_COMPOUND);
        for (int i=0;i<motionTags.size();i++) { CompoundTag value=motionTags.getCompound(i); List<BattleCell> path=new ArrayList<>(); ListTag points=value.getList("path",Tag.TAG_COMPOUND); for(int j=0;j<points.size();j++) path.add(loadCell(points.getCompound(j))); if(!path.isEmpty()) motions.add(new BattleActionTimeline.MoveAction(value.getUUID("actor"),loadCell(value.getCompound("start")),path,value.getInt("duration"),value.getInt("elapsed"),!value.contains("consumes_movement",Tag.TAG_BYTE)||value.getBoolean("consumes_movement"))); }
        Map<UUID,Integer> actionLocks=new LinkedHashMap<>();ListTag lockTags=tag.getList("action_locks",Tag.TAG_COMPOUND);for(int i=0;i<lockTags.size();i++){CompoundTag value=lockTags.getCompound(i);actionLocks.put(value.getUUID("actor"),value.getInt("ticks"));}
        Map<UUID, BattleSession.PendingDash> pendingDashes = new LinkedHashMap<>(); ListTag dashTags = tag.getList("pending_dashes", Tag.TAG_COMPOUND);
        for (int i = 0; i < dashTags.size(); i++) { CompoundTag value = dashTags.getCompound(i); List<UUID> targets = new ArrayList<>(); ListTag targetTags = value.getList("targets", Tag.TAG_COMPOUND); for (int j = 0; j < targetTags.size(); j++) targets.add(targetTags.getCompound(j).getUUID("id")); pendingDashes.put(value.getUUID("actor"), new BattleSession.PendingDash(targets)); }
        Map<UUID, BattleSession.PersistedReward> rewards = new LinkedHashMap<>(); ListTag rewardTags=tag.getList("rewards",Tag.TAG_COMPOUND);
        for(int i=0;i<rewardTags.size();i++){CompoundTag value=rewardTags.getCompound(i);List<String> candidates=new ArrayList<>();ListTag values=value.getList("candidates",Tag.TAG_STRING);for(int j=0;j<values.size();j++)candidates.add(values.getString(j));rewards.put(value.getUUID("player"),new BattleSession.PersistedReward(candidates,value.contains("selected",Tag.TAG_STRING)?value.getString("selected"):null,value.getBoolean("confirmed"),value.getInt("gold")));}
        Map<UUID, BattleSession.PersistedStatistics> statistics = new LinkedHashMap<>(); ListTag statisticTags=tag.getList("statistics",Tag.TAG_COMPOUND);
        for(int i=0;i<statisticTags.size();i++){CompoundTag value=statisticTags.getCompound(i);statistics.put(value.getUUID("player"),new BattleSession.PersistedStatistics(value.getDouble("dealt"),value.getDouble("taken"),value.getDouble("healing"),value.getInt("cards")));}
        BattleResult result = tag.contains("outcome",Tag.TAG_STRING) ? new BattleResult(session.id(), BattleResult.Outcome.valueOf(tag.getString("outcome")),
                session.combatants().stream().filter(c -> !c.downed()).map(Combatant::id).collect(java.util.stream.Collectors.toSet()),
                session.combatants().stream().filter(Combatant::downed).map(Combatant::id).collect(java.util.stream.Collectors.toSet()), Set.of("elimination"), Map.of()) : null;
        session.restoreRuntime(motions, actionLocks, skipped, result, rewards, statistics, tag.getDouble("phase_damage"), tag.getLong("battle_ticks"), pendingDashes);
        return session;
    }

    private static CompoundTag saveCombatant(Combatant combatant) {
        CompoundTag tag = new CompoundTag(); tag.putUUID("id", combatant.id()); if (combatant.playerId() != null) tag.putUUID("player", combatant.playerId());
        tag.putString("name", combatant.name()); tag.putString("faction", combatant.factionId()); saveCell(tag, combatant.cell());
        ListTag attributes = new ListTag(); combatant.attributes().forEach(attribute -> attributes.add(StringTag.valueOf(attribute.name()))); tag.put("attributes", attributes);
        tag.putFloat("max_health", combatant.maxHealth()); tag.putFloat("health", combatant.health()); tag.putFloat("max_mana", combatant.maxMana()); tag.putFloat("mana", combatant.mana()); tag.putFloat("mana_per_phase", combatant.manaPerPhase());
        tag.putDouble("initiative", combatant.initiative()); tag.putInt("movement_points", combatant.movementPoints()); tag.putInt("movement_remaining", combatant.movementRemaining()); tag.putInt("action_points", combatant.actionPoints()); tag.putInt("action_points_remaining", combatant.actionPointsRemaining()); tag.putInt("draw_per_phase", combatant.drawPerPhase());
        tag.putBoolean("downed", combatant.downed()); tag.putBoolean("auto", combatant.autoBattle()); tag.putBoolean("basic", combatant.basicAttackAvailable());
        tag.putFloat("block", combatant.block()); tag.putInt("strength", combatant.strength());
        tag.putInt("strength_level", combatant.strengthLevel()); tag.putDouble("weapon_attack", combatant.weaponAttack());
        tag.putString("weapon_family", combatant.weaponFamily().name());
        tag.putBoolean("unlimited_ap", combatant.unlimitedActionPoints());
        tag.putInt("dodge_dx", combatant.dodgeDx()); tag.putInt("dodge_dz", combatant.dodgeDz());
        tag.putInt("intercept_dx", combatant.interceptDx()); tag.putInt("intercept_dz", combatant.interceptDz());
        tag.putInt("item_use_limit", combatant.itemUseLimit()); tag.putInt("item_uses_remaining", combatant.itemUsesRemaining());
        tag.putInt("active_weapon_slot", combatant.activeWeaponSlot()); tag.putString("weapon_slot_1", combatant.weaponItem(1)); tag.putString("weapon_slot_2", combatant.weaponItem(2));
        tag.putInt("attack_card_count", combatant.attackCardCount());
        ListTag passiveCounters = new ListTag(); combatant.passiveCounters().forEach((id, count) -> {
            CompoundTag value = new CompoundTag(); value.putString("id", id); value.putInt("count", count); passiveCounters.add(value);
        }); tag.put("passive_counters", passiveCounters);
        ListTag powers = new ListTag(); combatant.usedPowers().forEach(id -> { CompoundTag value = new CompoundTag(); value.putString("id", id); powers.add(value); }); tag.put("used_powers", powers);
        ListTag additions = new ListTag(); combatant.permanentCardAdditions().forEach(id -> { CompoundTag value = new CompoundTag(); value.putString("id", id); additions.add(value); }); tag.put("permanent_cards", additions);
        ListTag statuses = new ListTag(); combatant.statuses().forEach(status -> { CompoundTag value = new CompoundTag(); value.putString("id", status.id()); value.putString("name", status.nameKey()); value.putInt("stacks", status.stacks()); value.putInt("rounds", status.remainingRounds()); value.putBoolean("beneficial", status.beneficial()); value.putBoolean("permanent", status.permanent()); value.putBoolean("potion", status.potion()); value.putBoolean("preserves_potion_level", status.preservesPotionLevel()); statuses.add(value); }); tag.put("statuses", statuses);
        saveCards(tag, "draw", combatant.deck().drawPile()); saveCards(tag, "hand", combatant.deck().hand()); saveCards(tag, "discard", combatant.deck().discardPile());
        saveCards(tag, "exhausted", combatant.deck().exhaustedPile()); saveCards(tag, "innate", combatant.deck().innateCards()); return tag;
    }
    private static Combatant loadCombatant(CompoundTag tag, long seed) {
        UUID player = tag.hasUUID("player") ? tag.getUUID("player") : null;
        DeckState deck = new DeckState(loadCards(tag, "draw"), loadCards(tag, "hand"), loadCards(tag, "discard"), loadCards(tag, "exhausted"), loadCards(tag, "innate"), seed ^ tag.getUUID("id").getMostSignificantBits());
        int actionPoints=tag.contains("action_points", Tag.TAG_INT)?tag.getInt("action_points"):(player==null?3:0);
        Set<CombatantAttribute> attributes = EnumSet.noneOf(CombatantAttribute.class); ListTag attributeTags = tag.getList("attributes", Tag.TAG_STRING);
        for (int i = 0; i < attributeTags.size(); i++) try { attributes.add(CombatantAttribute.valueOf(attributeTags.getString(i))); } catch (IllegalArgumentException ignored) { }
        Combatant combatant = new Combatant(tag.getUUID("id"), player, tag.getString("name"), tag.getString("faction"), loadCell(tag), tag.getFloat("max_health"), tag.getFloat("health"), tag.getFloat("max_mana"), tag.getFloat("mana"), tag.getFloat("mana_per_phase"), tag.getDouble("initiative"), tag.getInt("movement_points"), tag.getInt("movement_remaining"), tag.getBoolean("downed"), tag.getBoolean("auto"), tag.getBoolean("basic"), deck, actionPoints, tag.contains("draw_per_phase",Tag.TAG_INT)?tag.getInt("draw_per_phase"):DeckState.DRAW_PER_PHASE, tag.contains("action_points_remaining",Tag.TAG_INT)?tag.getInt("action_points_remaining"):actionPoints, attributes);
        combatant.addBlock(tag.getFloat("block")); combatant.addStrength(tag.getInt("strength"));
        combatant.configureEquipment(tag.getString("weapon_slot_1"), tag.getString("weapon_slot_2"), tag.getInt("active_weapon_slot"));
        if (tag.contains("strength_level", Tag.TAG_INT)) combatant.setStrengthLevel(tag.getInt("strength_level"));
        if (tag.contains("weapon_attack", Tag.TAG_DOUBLE)) combatant.setWeaponAttack(tag.getDouble("weapon_attack"));
        if (tag.contains("weapon_family", Tag.TAG_STRING)) {
            try { combatant.setWeaponFamily(net.exmo.exworld.battle.weapon.WeaponFamily.valueOf(tag.getString("weapon_family"))); }
            catch (IllegalArgumentException ignored) {}
        }
        boolean unlimited = tag.contains("unlimited_ap", Tag.TAG_BYTE) ? tag.getBoolean("unlimited_ap")
                : player != null && actionPoints == 0;
        combatant.setUnlimitedActionPoints(unlimited);
        combatant.setDodgeDirection(tag.getInt("dodge_dx"), tag.getInt("dodge_dz"));
        combatant.setInterceptDirection(tag.getInt("intercept_dx"), tag.getInt("intercept_dz"));
        combatant.setItemUseLimit(tag.contains("item_use_limit", Tag.TAG_INT) ? tag.getInt("item_use_limit") : 1);
        combatant.setItemUsesRemaining(tag.contains("item_uses_remaining", Tag.TAG_INT) ? tag.getInt("item_uses_remaining") : combatant.itemUseLimit());
        combatant.setAttackCardCount(tag.contains("attack_card_count", Tag.TAG_INT) ? tag.getInt("attack_card_count") : 0);
        Map<String, Integer> passiveCounters = new LinkedHashMap<>(); ListTag counterTags = tag.getList("passive_counters", Tag.TAG_COMPOUND);
        for (int i = 0; i < counterTags.size(); i++) { CompoundTag value = counterTags.getCompound(i); passiveCounters.put(value.getString("id"), value.getInt("count")); }
        combatant.restorePassiveCounters(passiveCounters);
        Set<String> powers = new LinkedHashSet<>(); ListTag powerTags = tag.getList("used_powers", Tag.TAG_COMPOUND); for (int i = 0; i < powerTags.size(); i++) powers.add(powerTags.getCompound(i).getString("id")); combatant.restoreUsedPowers(powers);
        Set<String> additions = new LinkedHashSet<>(); ListTag additionTags = tag.getList("permanent_cards", Tag.TAG_COMPOUND); for (int i = 0; i < additionTags.size(); i++) additions.add(additionTags.getCompound(i).getString("id")); combatant.restorePermanentCardAdditions(additions);
        List<BattleStatus> statuses = new ArrayList<>(); ListTag statusTags = tag.getList("statuses", Tag.TAG_COMPOUND); for (int i = 0; i < statusTags.size(); i++) { CompoundTag value = statusTags.getCompound(i); statuses.add(new BattleStatus(value.getString("id"), value.getString("name"), value.getInt("stacks"), value.getInt("rounds"), value.getBoolean("beneficial"), value.getBoolean("permanent"), value.getBoolean("potion"), value.getBoolean("preserves_potion_level"))); } combatant.restoreStatuses(statuses);
        return combatant;
    }
    private static void saveCards(CompoundTag parent, String key, List<SkillCard> cards) { ListTag tags = new ListTag(); cards.forEach(card -> { CompoundTag tag = new CompoundTag(); tag.putUUID("id", card.instanceId()); tag.putString("skill", card.skillId()); tag.putInt("star", card.star()); tag.putBoolean("innate", card.innate()); tag.putBoolean("exhaust", card.exhaust()); tags.add(tag); }); parent.put(key, tags); }
    private static List<SkillCard> loadCards(CompoundTag parent, String key) { List<SkillCard> cards = new ArrayList<>(); ListTag tags = parent.getList(key, Tag.TAG_COMPOUND); for (int i = 0; i < tags.size(); i++) { CompoundTag tag = tags.getCompound(i); cards.add(new SkillCard(tag.getUUID("id"), tag.getString("skill"), Math.max(1, tag.getInt("star")), tag.getBoolean("innate"), tag.getBoolean("exhaust"))); } return cards; }
    private static void saveCell(CompoundTag tag, BattleCell cell) { tag.putInt("cell_x", cell.x()); tag.putInt("cell_z", cell.z()); tag.putInt("cell_y", cell.floorY()); }
    private static BattleCell loadCell(CompoundTag tag) { return new BattleCell(tag.getInt("cell_x"), tag.getInt("cell_z"), tag.getInt("cell_y")); }
}
