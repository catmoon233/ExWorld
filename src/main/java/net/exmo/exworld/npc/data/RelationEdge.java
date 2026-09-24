package net.exmo.exworld.npc.data;

/** Directed, editable relation. The type is a free string so the graph is not a fixed enum. */
public record RelationEdge(String fromId, String toId, String type, int affinity, String note) {
    public RelationEdge {
        fromId = fromId == null ? "" : fromId.trim();
        toId = toId == null ? "" : toId.trim();
        type = type == null ? "" : type.trim();
        affinity = Math.max(-100, Math.min(100, affinity));
        note = note == null ? "" : note;
    }

    public boolean ally() {
        return "ally".equalsIgnoreCase(type) || "盟友".equals(type) || affinity >= 30;
    }
}
