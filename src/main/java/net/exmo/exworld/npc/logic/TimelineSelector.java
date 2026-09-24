package net.exmo.exworld.npc.logic;

import net.exmo.exworld.npc.data.TimelineNode;

import java.util.List;
import java.util.Optional;

/** Picks the active timeline window. Higher priority wins, then the later start. */
public final class TimelineSelector {
    private TimelineSelector() {}

    public static Optional<TimelineNode> select(List<TimelineNode> nodes, int minute) {
        if (nodes == null || nodes.isEmpty()) return Optional.empty();
        int now = Math.floorMod(minute, 1440);
        TimelineNode best = null;
        for (TimelineNode node : nodes) {
            if (node == null || !contains(node, now)) continue;
            if (best == null || node.priority() > best.priority()
                    || (node.priority() == best.priority() && node.startMinute() > best.startMinute())) {
                best = node;
            }
        }
        return Optional.ofNullable(best);
    }

    public static boolean contains(TimelineNode node, int minute) {
        if (node == null || node.durationMinutes() <= 0) return false;
        int start = Math.floorMod(node.startMinute(), 1440);
        int duration = node.durationMinutes();
        int now = Math.floorMod(minute, 1440);
        if (duration >= 1440) return true;
        int end = start + duration;
        if (end <= 1440) return now >= start && now < end;
        return now >= start || now < end - 1440;
    }
}
