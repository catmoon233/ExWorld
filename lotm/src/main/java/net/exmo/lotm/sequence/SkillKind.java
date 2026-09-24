package net.exmo.lotm.sequence;

public enum SkillKind {
    ACTIVE,
    PASSIVE;

    public static SkillKind parse(String raw) {
        if (raw == null) return PASSIVE;
        return "active".equalsIgnoreCase(raw) ? ACTIVE : PASSIVE;
    }

    public String token() {
        return name().toLowerCase();
    }
}
