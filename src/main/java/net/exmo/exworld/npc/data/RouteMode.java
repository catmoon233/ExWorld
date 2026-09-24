package net.exmo.exworld.npc.data;

public enum RouteMode {
    PATHFIND,
    MANUAL;

    public static RouteMode parse(String raw) {
        if (raw != null && raw.equalsIgnoreCase("MANUAL")) return MANUAL;
        return PATHFIND;
    }
}
