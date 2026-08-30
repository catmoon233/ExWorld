package net.exmo.exworld.battle;

import net.exmo.exworld.battle.api.*;
import net.exmo.exworld.battle.arena.*;
import net.exmo.exworld.battle.skill.SkillRegistry;
import net.exmo.exworld.battle.combat.BattleEffectResolver;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;

import java.util.*;
import java.util.function.Consumer;

/** In-memory coordinator. Persistence and Minecraft entity lifecycle are adapters around this interface. */
public final class BattleEngine implements BattleInterface {
    private final Map<BattleId, BattleSession> sessions = new LinkedHashMap<>();
    private final Map<String, ArenaDefinition> arenas = new LinkedHashMap<>();
    private final SkillRegistry skills;
    private final BattleEffectResolver effects;
    private int nextSlot;
    private final Map<BattleId, Integer> assignedSlots = new HashMap<>();
    private final NavigableSet<Integer> freeSlots = new TreeSet<>();
    private Consumer<BattleResult> resultConsumer = ignored -> {};
    private Consumer<BattleEvent> eventConsumer = ignored -> {};

    public BattleEngine(SkillRegistry skills) { this(skills,BattleEffectResolver.logical()); }
    public BattleEngine(SkillRegistry skills, BattleEffectResolver effects) {
        this.skills = skills; this.effects=effects;
        for (int size : new int[]{18, 27, 54, 108}) registerArena(ArenaDefinition.flat("exworld:flat_" + size, size, 64));
    }

    public void registerArena(ArenaDefinition definition) { arenas.put(definition.id(), definition); }
    public void onResult(Consumer<BattleResult> consumer) { resultConsumer = consumer; }
    public void onEvent(Consumer<BattleEvent> consumer) { eventConsumer = consumer; }
    @Override public IEventBus events() { return NeoForge.EVENT_BUS; }

    @Override public BattleId startEncounter(EncounterRequest request) {
        ArenaDefinition definition = Optional.ofNullable(arenas.get(request.arenaId()))
                .orElseThrow(() -> new IllegalArgumentException("Unknown arena " + request.arenaId()));
        BattleId id = BattleId.create();
        int slot = freeSlots.isEmpty() ? nextSlot++ : freeSlots.pollFirst();
        int originX = request.host().originX();
        int originZ = request.host().originZ();
        if (request.host().ownsArena()) {
            originX = (slot % 64) * 256;
            originZ = (slot / 64) * 256;
        }
        BattleSession session = new BattleSession(id, request, new ArenaGrid(definition), skills.snapshot(), originX, originZ, effects);
        session.onResult(resultConsumer); session.onEvent(eventConsumer);
        sessions.put(id, session); assignedSlots.put(id, slot); return id;
    }

    @Override public CommandReceipt submit(BattleCommand command) {
        BattleSession session = sessions.get(command.battleId());
        return session == null ? CommandReceipt.rejected(command.commandId(), "battle.command.unknown_session", -1)
                : session.submit(command);
    }
    @Override public Optional<BattleSnapshot> snapshot(BattleId battleId) { return Optional.ofNullable(sessions.get(battleId)).map(BattleSession::snapshot); }
    public Optional<BattleSession> session(BattleId battleId) { return Optional.ofNullable(sessions.get(battleId)); }
    public Collection<BattleSession> sessions() { return List.copyOf(sessions.values()); }
    public void tick() { List.copyOf(sessions.values()).forEach(BattleSession::tick); }
    public void remove(BattleId id) {
        sessions.remove(id);
        Integer slot = assignedSlots.remove(id); if (slot != null) freeSlots.add(slot);
    }
    public void clear() { sessions.clear(); assignedSlots.clear(); freeSlots.clear(); nextSlot = 0; }
    public void restore(BattleSession session) {
        sessions.put(session.id(), session);
        int slot = Math.max(0, session.snapshot().arenaOriginZ() / 256 * 64 + session.snapshot().arenaOriginX() / 256);
        nextSlot = Math.max(nextSlot, slot + 1);
        assignedSlots.put(session.id(), slot); freeSlots.remove(slot);
        session.onResult(resultConsumer);
        session.onEvent(eventConsumer);
    }
    public Optional<ArenaDefinition> arena(String id) { return Optional.ofNullable(arenas.get(id)); }
    public void replaceArenas(Set<String> previousIds, Collection<ArenaDefinition> definitions) {
        Map<String, ArenaDefinition> next = new LinkedHashMap<>(arenas);
        previousIds.forEach(next::remove);
        definitions.forEach(value -> next.put(value.id(), value));
        arenas.clear(); arenas.putAll(next);
    }
}
