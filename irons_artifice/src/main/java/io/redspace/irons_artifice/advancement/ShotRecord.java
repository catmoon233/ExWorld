package io.redspace.irons_artifice.advancement;

import java.util.UUID;

/**
 * Server-only identity for one fired projectile and the trigger-pull it came from.
 */
public final class ShotRecord {
    private final UUID fireId;
    private final boolean fullMagazine;
    private boolean ricocheted;

    private ShotRecord(UUID fireId, boolean fullMagazine, boolean ricocheted) {
        this.fireId = fireId;
        this.fullMagazine = fullMagazine;
        this.ricocheted = ricocheted;
    }

    public static ShotRecord of(UUID fireId, boolean fullMagazine) {
        return new ShotRecord(fireId, fullMagazine, false);
    }

    public ShotRecord child(boolean ricocheted) {
        return new ShotRecord(fireId,  fullMagazine, this.ricocheted || ricocheted);
    }

    public UUID fireId() {
        return fireId;
    }

    public boolean fullMagazine() {
        return fullMagazine;
    }

    public boolean ricocheted() {
        return ricocheted;
    }

    public void markRicocheted() {
        this.ricocheted = true;
    }
}
