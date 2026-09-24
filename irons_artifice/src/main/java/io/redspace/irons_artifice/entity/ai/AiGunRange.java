package io.redspace.irons_artifice.entity.ai;

public record AiGunRange(float maxRange, float panicRange, float idealRangeMin, float idealRangeMax, float bayonetRange) {
    public static final float DEFAULT_BAYONET_RANGE = 10f;

    public AiGunRange(float maxRange) {
        this(maxRange, maxRange * 0.125f, maxRange * 0.35f, maxRange * 0.75f, DEFAULT_BAYONET_RANGE);
    }

    public float panicSqr() {
        return panicRange * panicRange;
    }

    public float bayonetSqr() {
        return bayonetRange * bayonetRange;
    }

    public float idealMinSqr() {
        return idealRangeMin * idealRangeMin;
    }

    public float idealMaxSqr() {
        return idealRangeMax * idealRangeMax;
    }

    public float maxRangeSqr() {
        return maxRange * maxRange;
    }
}
