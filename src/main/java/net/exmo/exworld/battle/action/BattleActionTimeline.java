package net.exmo.exworld.battle.action;

import net.exmo.exworld.battle.model.BattleCell;

import java.util.*;

/**
 * Server-authoritative action clock. It owns presentation duration while callers own the final world side effect.
 * Logical reservations are made before an action is added; the destination is committed exactly once on completion.
 */
public final class BattleActionTimeline {
    public static final int TICKS_PER_CELL = 5;
    private final Map<UUID, MoveAction> moves = new LinkedHashMap<>();

    public boolean moving(UUID actorId) { return moves.containsKey(actorId); }
    public Collection<MoveAction> moves() { return List.copyOf(moves.values()); }

    public boolean startMove(UUID actorId, BattleCell start, List<BattleCell> path) {
        if (path.isEmpty() || moves.containsKey(actorId)) return false;
        moves.put(actorId, new MoveAction(actorId, start, path, path.size() * TICKS_PER_CELL, 0, true));
        return true;
    }

    public boolean startFreeMove(UUID actorId, BattleCell start, List<BattleCell> path) {
        if (path.isEmpty() || moves.containsKey(actorId)) return false;
        moves.put(actorId, new MoveAction(actorId, start, path, path.size() * TICKS_PER_CELL, 0, false));
        return true;
    }

    public List<MoveAction> tick() {
        List<MoveAction> completed = new ArrayList<>();
        Iterator<Map.Entry<UUID, MoveAction>> iterator = moves.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, MoveAction> entry = iterator.next();
            MoveAction next = entry.getValue().advance();
            if (next.complete()) {
                completed.add(next);
                iterator.remove();
            } else entry.setValue(next);
        }
        return completed;
    }

    public void restore(Collection<MoveAction> restored) {
        moves.clear();
        restored.forEach(move -> moves.put(move.actorId(), move));
    }

    public record MoveAction(UUID actorId, BattleCell start, List<BattleCell> path, int durationTicks, int elapsedTicks,
                             boolean consumesMovement) {
        public MoveAction(UUID actorId, BattleCell start, List<BattleCell> path, int durationTicks, int elapsedTicks) {
            this(actorId, start, path, durationTicks, elapsedTicks, true);
        }
        public MoveAction {
            path = List.copyOf(path);
            if (path.isEmpty()) throw new IllegalArgumentException("move path cannot be empty");
            durationTicks = Math.max(1, durationTicks);
            elapsedTicks = Math.max(0, Math.min(durationTicks, elapsedTicks));
        }
        public BattleCell destination() { return path.getLast(); }
        public boolean complete() { return elapsedTicks >= durationTicks; }
        public MoveAction advance() { return new MoveAction(actorId, start, path, durationTicks, elapsedTicks + 1, consumesMovement); }
        public double progress(float partialTick) {
            return Math.min(1.0, (elapsedTicks + Math.max(0, partialTick)) / durationTicks);
        }
        public Segment segment(float partialTick) {
            double scaled = progress(partialTick) * path.size();
            int index = Math.min(path.size() - 1, (int) Math.floor(scaled));
            BattleCell from = index == 0 ? start : path.get(index - 1);
            return new Segment(from, path.get(index), Math.min(1.0, scaled - index));
        }
    }

    public record Segment(BattleCell from, BattleCell to, double progress) {}
}
