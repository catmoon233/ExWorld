package net.exmo.exworld.network;

import net.exmo.exworld.Exworld;
import net.exmo.exworld.battle.api.BattleId;
import net.exmo.exworld.battle.api.BattleEvent;
import net.exmo.exworld.battle.api.BattleSnapshot;
import net.exmo.exworld.battle.model.BattleCell;
import net.exmo.exworld.battle.model.CombatantAttribute;
import net.exmo.exworld.battle.model.BattleState;
import net.exmo.exworld.battle.skill.SkillDefinition;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

public record BattleSnapshotPayload(BattleSnapshot snapshot) implements CustomPacketPayload {
    public static final Type<BattleSnapshotPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "battle_snapshot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BattleSnapshotPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> encode(buffer, payload.snapshot), buffer -> new BattleSnapshotPayload(decode(buffer)));

    private static void encode(RegistryFriendlyByteBuf buffer, BattleSnapshot snapshot) {
        buffer.writeUUID(snapshot.battleId().value()); buffer.writeVarLong(snapshot.revision()); buffer.writeVarLong(snapshot.eventSequence());
        buffer.writeEnum(snapshot.state()); buffer.writeUtf(snapshot.arenaId()); buffer.writeVarInt(snapshot.arenaSize());
        buffer.writeUtf(snapshot.biomeId());
        buffer.writeVarInt(snapshot.arenaOriginX()); buffer.writeVarInt(snapshot.arenaOriginZ());
        buffer.writeVarInt(snapshot.blockedCells().size()); snapshot.blockedCells().forEach(cell -> writeCell(buffer, cell));
        buffer.writeVarInt(snapshot.round());
        buffer.writeUtf(snapshot.activeFaction()); buffer.writeVarInt(snapshot.factionOrder().size());
        snapshot.factionOrder().forEach(buffer::writeUtf); buffer.writeVarInt(snapshot.phaseTicksRemaining());
        buffer.writeVarInt(snapshot.combatants().size());
        for (BattleSnapshot.CombatantView view : snapshot.combatants().values()) {
            buffer.writeUUID(view.id()); buffer.writeBoolean(view.playerId() != null); if (view.playerId() != null) buffer.writeUUID(view.playerId());
            buffer.writeUtf(view.name()); buffer.writeUtf(view.factionId()); writeCell(buffer, view.cell()); buffer.writeVarInt(view.attributes().size()); view.attributes().forEach(buffer::writeEnum);
            buffer.writeFloat(view.health()); buffer.writeFloat(view.maxHealth()); buffer.writeFloat(view.block()); buffer.writeFloat(view.mana()); buffer.writeFloat(view.maxMana());
            buffer.writeVarInt(view.movementRemaining()); buffer.writeVarInt(view.actionPoints()); buffer.writeVarInt(view.actionPointsRemaining()); buffer.writeBoolean(view.downed()); buffer.writeBoolean(view.autoBattle());
            buffer.writeVarInt(view.hand().size());
            for (BattleSnapshot.CardView card : view.hand()) {
                buffer.writeUUID(card.instanceId()); buffer.writeUtf(card.skillId()); buffer.writeUtf(card.nameKey()); buffer.writeUtf(card.descriptionKey()); buffer.writeUtf(card.icon());
                buffer.writeBoolean(card.innate()); buffer.writeVarInt(card.star()); buffer.writeVarInt(card.manaCost()); buffer.writeVarInt(card.range());
                buffer.writeEnum(card.targetType()); buffer.writeBoolean(card.playable()); buffer.writeBoolean(card.retained());
                buffer.writeEnum(card.type()); buffer.writeVarInt(card.keywords().size()); card.keywords().forEach(buffer::writeEnum);
                buffer.writeBoolean(card.chebyshevRange());
            }
            writePile(buffer,view.drawPile());writePile(buffer,view.discardPile());
            buffer.writeVarInt(view.statuses().size());
            for (BattleSnapshot.StatusView status : view.statuses()) {
                buffer.writeUtf(status.id()); buffer.writeUtf(status.nameKey()); buffer.writeVarInt(status.stacks());
                buffer.writeVarInt(status.remainingRounds()); buffer.writeBoolean(status.beneficial());
            }
            buffer.writeVarInt(view.itemUseLimit()); buffer.writeVarInt(view.itemUsesRemaining()); buffer.writeVarInt(view.activeWeaponSlot());
            buffer.writeVarInt(view.weaponSlots().size());
            for (BattleSnapshot.WeaponSlotView weapon : view.weaponSlots()) {
                buffer.writeVarInt(weapon.slot()); buffer.writeUtf(weapon.itemId()); buffer.writeUtf(weapon.displayName());
                buffer.writeBoolean(weapon.available()); buffer.writeUtf(weapon.passiveId()); buffer.writeVarInt(weapon.passiveProgress());
            }
            buffer.writeVarInt(view.items().size());
            for (BattleSnapshot.ItemView item : view.items()) {
                buffer.writeVarInt(item.inventorySlot()); buffer.writeUtf(item.itemId()); buffer.writeUtf(item.displayName());
                buffer.writeVarInt(item.count()); buffer.writeBoolean(item.usable()); buffer.writeUtf(item.targetType());
            }
        }
        buffer.writeVarInt(snapshot.readyPlayers().size()); snapshot.readyPlayers().forEach(buffer::writeUUID);
        buffer.writeVarInt(snapshot.events().size());
        for (BattleEvent event : snapshot.events()) {
            buffer.writeVarLong(event.sequence()); buffer.writeVarInt(event.round()); buffer.writeEnum(event.type());
            buffer.writeUUID(event.actorId()); buffer.writeBoolean(event.targetId() != null); if (event.targetId() != null) buffer.writeUUID(event.targetId());
            buffer.writeUtf(event.actorName()); buffer.writeUtf(event.targetName()); buffer.writeUtf(event.skillId());
            buffer.writeUtf(event.skillNameKey()); buffer.writeDouble(event.amount());
            buffer.writeBoolean(event.fromCell() != null); if (event.fromCell() != null) writeCell(buffer, event.fromCell());
            buffer.writeBoolean(event.toCell() != null); if (event.toCell() != null) writeCell(buffer, event.toCell());
        }
        buffer.writeVarInt(snapshot.motions().size());
        for (BattleSnapshot.MotionView motion : snapshot.motions()) {
            buffer.writeUUID(motion.actorId()); writeCell(buffer, motion.start()); buffer.writeVarInt(motion.path().size());
            motion.path().forEach(cell -> writeCell(buffer, cell)); buffer.writeVarInt(motion.durationTicks()); buffer.writeVarInt(motion.elapsedTicks());
        }
        buffer.writeBoolean(snapshot.intro() != null);
        if (snapshot.intro() != null) {
            buffer.writeVarInt(snapshot.intro().elapsedTicks()); buffer.writeVarInt(snapshot.intro().totalTicks());
            buffer.writeVarInt(snapshot.intro().skippedPlayers().size()); snapshot.intro().skippedPlayers().forEach(buffer::writeUUID);
            buffer.writeVarInt(snapshot.intro().requiredPlayers());
        }
        buffer.writeBoolean(snapshot.result() != null);
        if (snapshot.result() != null) {
            buffer.writeUtf(snapshot.result().outcome()); buffer.writeVarInt(snapshot.result().ticksRemaining());
            buffer.writeVarInt(snapshot.result().rounds()); buffer.writeVarLong(snapshot.result().durationTicks()); buffer.writeVarInt(snapshot.result().downedUnits());
            buffer.writeVarInt(snapshot.result().players().size());
            snapshot.result().players().forEach((playerId, result) -> {
                buffer.writeUUID(playerId); buffer.writeVarInt(result.candidates().size()); result.candidates().forEach(buffer::writeUtf);
                buffer.writeBoolean(result.selectedCandidate() != null); if (result.selectedCandidate() != null) buffer.writeUtf(result.selectedCandidate());
                buffer.writeBoolean(result.confirmed()); buffer.writeVarInt(result.gold()); buffer.writeDouble(result.damageDealt());
                buffer.writeDouble(result.damageTaken()); buffer.writeDouble(result.healing()); buffer.writeVarInt(result.cardsUsed());
            });
        }
        buffer.writeDouble(snapshot.phaseDamage());
    }

    private static BattleSnapshot decode(RegistryFriendlyByteBuf buffer) {
        BattleId id = new BattleId(buffer.readUUID()); long revision = buffer.readVarLong(); long sequence = buffer.readVarLong();
        BattleState state = buffer.readEnum(BattleState.class); String arenaId = buffer.readUtf(); int size = buffer.readVarInt(); String biomeId = buffer.readUtf();
        int originX = buffer.readVarInt(), originZ = buffer.readVarInt();
        int blockedCount = buffer.readVarInt(); List<BattleCell> blockedCells = new ArrayList<>(blockedCount);
        for (int i = 0; i < blockedCount; i++) blockedCells.add(readCell(buffer));
        int round = buffer.readVarInt(); String active = buffer.readUtf();
        int orderSize = buffer.readVarInt(); List<String> order = new ArrayList<>(orderSize); for (int i = 0; i < orderSize; i++) order.add(buffer.readUtf());
        int ticks = buffer.readVarInt(), combatantCount = buffer.readVarInt(); Map<UUID, BattleSnapshot.CombatantView> views = new LinkedHashMap<>();
        for (int i = 0; i < combatantCount; i++) {
            UUID combatantId = buffer.readUUID(); UUID playerId = buffer.readBoolean() ? buffer.readUUID() : null;
            String name = buffer.readUtf(), faction = buffer.readUtf(); BattleCell cell = readCell(buffer); int attributeCount = buffer.readVarInt(); Set<CombatantAttribute> attributes = EnumSet.noneOf(CombatantAttribute.class); for (int attribute = 0; attribute < attributeCount; attribute++) attributes.add(buffer.readEnum(CombatantAttribute.class));
            float health = buffer.readFloat(), maxHealth = buffer.readFloat(), block = buffer.readFloat(), mana = buffer.readFloat(), maxMana = buffer.readFloat();
            int movement = buffer.readVarInt(), actionPoints = buffer.readVarInt(), actionPointsRemaining = buffer.readVarInt(); boolean downed = buffer.readBoolean(), auto = buffer.readBoolean();
            int handSize = buffer.readVarInt(); List<BattleSnapshot.CardView> hand = new ArrayList<>(handSize);
            for (int card = 0; card < handSize; card++) hand.add(new BattleSnapshot.CardView(buffer.readUUID(), buffer.readUtf(),
                    buffer.readUtf(), buffer.readUtf(), buffer.readUtf(), buffer.readBoolean(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                    buffer.readEnum(SkillDefinition.TargetType.class), buffer.readBoolean(),buffer.readBoolean(), buffer.readEnum(net.exmo.exworld.battle.card.CardDefinition.CardType.class), readKeywords(buffer), buffer.readBoolean()));
            List<BattleSnapshot.CardView> draw=readPile(buffer),discard=readPile(buffer);
            int statusCount = buffer.readVarInt(); List<BattleSnapshot.StatusView> statuses = new ArrayList<>(statusCount);
            for (int status = 0; status < statusCount; status++) statuses.add(new BattleSnapshot.StatusView(buffer.readUtf(), buffer.readUtf(),
                    buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean()));
            int itemUseLimit = buffer.readVarInt(), itemUsesRemaining = buffer.readVarInt(), activeWeaponSlot = buffer.readVarInt();
            int weaponCount = buffer.readVarInt(); List<BattleSnapshot.WeaponSlotView> weapons = new ArrayList<>(weaponCount);
            for (int weapon = 0; weapon < weaponCount; weapon++) weapons.add(new BattleSnapshot.WeaponSlotView(buffer.readVarInt(), buffer.readUtf(),
                    buffer.readUtf(), buffer.readBoolean(), buffer.readUtf(), buffer.readVarInt()));
            int itemCount = buffer.readVarInt(); List<BattleSnapshot.ItemView> items = new ArrayList<>(itemCount);
            for (int item = 0; item < itemCount; item++) items.add(new BattleSnapshot.ItemView(buffer.readVarInt(), buffer.readUtf(), buffer.readUtf(),
                    buffer.readVarInt(), buffer.readBoolean(), buffer.readUtf()));
            views.put(combatantId, new BattleSnapshot.CombatantView(combatantId, playerId, name, faction, cell, attributes, health, maxHealth, block,
                    mana, maxMana, movement, actionPoints, actionPointsRemaining, downed, auto, hand, draw, discard, statuses, itemUseLimit, itemUsesRemaining,
                    activeWeaponSlot, weapons, items));
        }
        int readySize = buffer.readVarInt(); Set<UUID> ready = new LinkedHashSet<>(); for (int i = 0; i < readySize; i++) ready.add(buffer.readUUID());
        int eventCount = buffer.readVarInt(); List<BattleEvent> events = new ArrayList<>(eventCount);
        for (int i = 0; i < eventCount; i++) {
            long eventSequence = buffer.readVarLong(); int eventRound = buffer.readVarInt(); BattleEvent.Type type = buffer.readEnum(BattleEvent.Type.class);
            UUID actorId = buffer.readUUID(); UUID targetId = buffer.readBoolean() ? buffer.readUUID() : null;
            String actorName = buffer.readUtf(), targetName = buffer.readUtf(), skillId = buffer.readUtf(), skillName = buffer.readUtf(); double amount = buffer.readDouble();
            BattleCell from = buffer.readBoolean() ? readCell(buffer) : null; BattleCell to = buffer.readBoolean() ? readCell(buffer) : null;
            events.add(new BattleEvent(eventSequence, eventRound, type, actorId, targetId, actorName, targetName, skillId, skillName, amount, from, to));
        }
        int motionCount = buffer.readVarInt(); List<BattleSnapshot.MotionView> motions = new ArrayList<>(motionCount);
        for (int i = 0; i < motionCount; i++) {
            UUID actor = buffer.readUUID(); BattleCell start = readCell(buffer); int pathSize = buffer.readVarInt(); List<BattleCell> path = new ArrayList<>(pathSize);
            for (int cell = 0; cell < pathSize; cell++) path.add(readCell(buffer));
            motions.add(new BattleSnapshot.MotionView(actor, start, path, buffer.readVarInt(), buffer.readVarInt()));
        }
        BattleSnapshot.IntroView intro = null;
        if (buffer.readBoolean()) {
            int elapsed = buffer.readVarInt(), total = buffer.readVarInt(), skippedCount = buffer.readVarInt(); Set<UUID> skipped = new LinkedHashSet<>();
            for (int i = 0; i < skippedCount; i++) skipped.add(buffer.readUUID());
            intro = new BattleSnapshot.IntroView(elapsed, total, skipped, buffer.readVarInt());
        }
        BattleSnapshot.ResultView result = null;
        if (buffer.readBoolean()) {
            String outcome = buffer.readUtf(); int remaining = buffer.readVarInt(), rounds = buffer.readVarInt(); long duration = buffer.readVarLong(); int downed = buffer.readVarInt(); int resultCount = buffer.readVarInt();
            Map<UUID, BattleSnapshot.PlayerResultView> playerResults = new LinkedHashMap<>();
            for (int i = 0; i < resultCount; i++) {
                UUID playerId = buffer.readUUID(); int candidateCount = buffer.readVarInt(); List<String> candidates = new ArrayList<>();
                for (int candidate = 0; candidate < candidateCount; candidate++) candidates.add(buffer.readUtf());
                String selected = buffer.readBoolean() ? buffer.readUtf() : null; boolean confirmed = buffer.readBoolean(); int gold = buffer.readVarInt();
                playerResults.put(playerId, new BattleSnapshot.PlayerResultView(candidates, selected, confirmed, gold,
                        buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readVarInt()));
            }
            result = new BattleSnapshot.ResultView(outcome, remaining, rounds, duration, downed, playerResults);
        }
        double phaseDamage = buffer.readDouble();
        return new BattleSnapshot(id, revision, sequence, state, arenaId, size, originX, originZ, blockedCells, biomeId, round, active, order, ticks, views, ready, events,
                motions, intro, result, phaseDamage);
    }
    private static void writeCell(RegistryFriendlyByteBuf buffer, BattleCell cell) { buffer.writeVarInt(cell.x()); buffer.writeVarInt(cell.z()); buffer.writeVarInt(cell.floorY()); }
    private static void writePile(RegistryFriendlyByteBuf buffer,List<BattleSnapshot.CardView> pile){buffer.writeVarInt(pile.size());for(var card:pile){buffer.writeUUID(card.instanceId());buffer.writeUtf(card.skillId());buffer.writeUtf(card.nameKey());buffer.writeUtf(card.descriptionKey());buffer.writeUtf(card.icon());buffer.writeBoolean(card.innate());buffer.writeVarInt(card.star());buffer.writeVarInt(card.manaCost());buffer.writeVarInt(card.range());buffer.writeEnum(card.targetType());buffer.writeBoolean(card.playable());buffer.writeBoolean(card.retained());buffer.writeEnum(card.type());buffer.writeVarInt(card.keywords().size());card.keywords().forEach(buffer::writeEnum);buffer.writeBoolean(card.chebyshevRange());}}
    private static List<BattleSnapshot.CardView> readPile(RegistryFriendlyByteBuf buffer){int count=buffer.readVarInt();List<BattleSnapshot.CardView> pile=new ArrayList<>(count);for(int i=0;i<count;i++)pile.add(new BattleSnapshot.CardView(buffer.readUUID(),buffer.readUtf(),buffer.readUtf(),buffer.readUtf(),buffer.readUtf(),buffer.readBoolean(),buffer.readVarInt(),buffer.readVarInt(),buffer.readVarInt(),buffer.readEnum(SkillDefinition.TargetType.class),buffer.readBoolean(),buffer.readBoolean(),buffer.readEnum(net.exmo.exworld.battle.card.CardDefinition.CardType.class),readKeywords(buffer),buffer.readBoolean()));return pile;}
    private static Set<net.exmo.exworld.battle.card.CardDefinition.CardKeyword> readKeywords(RegistryFriendlyByteBuf buffer){int count=buffer.readVarInt();Set<net.exmo.exworld.battle.card.CardDefinition.CardKeyword> values=EnumSet.noneOf(net.exmo.exworld.battle.card.CardDefinition.CardKeyword.class);for(int i=0;i<count;i++)values.add(buffer.readEnum(net.exmo.exworld.battle.card.CardDefinition.CardKeyword.class));return values;}
    private static BattleCell readCell(RegistryFriendlyByteBuf buffer) { return new BattleCell(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt()); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
