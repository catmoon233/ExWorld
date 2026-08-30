package net.exmo.exworld.battle.model;

/** A battle-local physical property with tactical and presentation consequences. */
public enum CombatantAttribute {
    /** Occupies its tactical cell but flies three blocks above its floor and cannot occlude ranged line of sight. */
    AIRBORNE(3.0D, false);

    private final double elevation;
    private final boolean blocksRangedLineOfSight;

    CombatantAttribute(double elevation, boolean blocksRangedLineOfSight) {
        this.elevation = elevation;
        this.blocksRangedLineOfSight = blocksRangedLineOfSight;
    }

    public double elevation() { return elevation; }
    public boolean blocksRangedLineOfSight() { return blocksRangedLineOfSight; }
}
