package net.exmo.exworld.npc.data;

/** One window on the day timeline. A window may cross midnight. */
public record TimelineNode(String id, int startMinute, int durationMinutes, String actionId, String placeId, int priority) {
    public TimelineNode {
        id = id == null ? "" : id.trim();
        startMinute = Math.floorMod(startMinute, 1440);
        durationMinutes = Math.max(0, Math.min(durationMinutes, 1440));
        actionId = actionId == null ? "" : actionId.trim();
        placeId = placeId == null ? "" : placeId.trim();
    }
}
