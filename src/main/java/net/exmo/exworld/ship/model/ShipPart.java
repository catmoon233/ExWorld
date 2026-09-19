package net.exmo.exworld.ship.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Named voxel set on a 飞船模板. Occupancy is the upgrade mask; speed is a simple engine attribute. */
public final class ShipPart {
    private final String id;
    private final String name;
    private final int color;
    private final int[] occupancy;
    private final List<String> allowedVariants;
    private final double speed;

    public ShipPart(String id, String name, int color, int[] occupancy, List<String> allowedVariants, double speed) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("part id is blank");
        this.id = id;
        this.name = name == null || name.isBlank() ? id : name;
        this.color = color;
        this.occupancy = occupancy == null ? new int[0] : ShipOccupancy.sorted(occupancy);
        this.allowedVariants = allowedVariants == null ? List.of()
                : Collections.unmodifiableList(new ArrayList<>(allowedVariants.stream().filter(value -> value != null && !value.isBlank()).distinct().toList()));
        this.speed = Math.max(0, speed);
    }

    public String id() { return id; }
    public String name() { return name; }
    public int color() { return color; }
    public int[] occupancy() { return occupancy.clone(); }
    public List<String> allowedVariants() { return allowedVariants; }
    public double speed() { return speed; }
    public boolean empty() { return occupancy.length == 0; }
    public boolean allows(String variantId) { return allowedVariants.contains(variantId); }

    public ShipPart withOccupancy(int[] next) {
        return new ShipPart(id, name, color, next, allowedVariants, speed);
    }

    public ShipPart withAllowedVariants(List<String> variants) {
        return new ShipPart(id, name, color, occupancy, variants, speed);
    }

    public ShipPart withName(String next) { return new ShipPart(id, next, color, occupancy, allowedVariants, speed); }
    public ShipPart withColor(int next) { return new ShipPart(id, name, next, occupancy, allowedVariants, speed); }
    public ShipPart withSpeed(double next) { return new ShipPart(id, name, color, occupancy, allowedVariants, next); }

    @Override public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof ShipPart part)) return false;
        return color == part.color && Double.compare(speed, part.speed) == 0 && id.equals(part.id) && name.equals(part.name)
                && java.util.Arrays.equals(occupancy, part.occupancy) && allowedVariants.equals(part.allowedVariants);
    }

    @Override public int hashCode() { return Objects.hash(id, name, color, java.util.Arrays.hashCode(occupancy), allowedVariants, speed); }
}
