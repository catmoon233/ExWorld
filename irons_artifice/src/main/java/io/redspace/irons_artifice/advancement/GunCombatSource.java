package io.redspace.irons_artifice.advancement;

import net.minecraft.util.StringRepresentable;

public enum GunCombatSource implements StringRepresentable {
    BULLET("bullet"),
    BAYONET("bayonet");

    public static final StringRepresentable.EnumCodec<GunCombatSource> CODEC = StringRepresentable.fromEnum(GunCombatSource::values);

    private final String name;

    GunCombatSource(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
