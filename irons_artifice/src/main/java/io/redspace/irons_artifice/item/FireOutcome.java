package io.redspace.irons_artifice.item;

public enum FireOutcome {
    FIRED,
    INVALID_SHOOTER,
    NO_GUN,
    FIRE_DELAY_ACTIVE,
    RELOADING,
    EMPTY_MAGAZINE,
    EVENT_CANCELLED;

    public boolean fired() {
        return this == FIRED;
    }
}
