package net.exmo.exworld.ship.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Saved 飞船模板: base hull, named parts and the current part-variant selection. */
public final class ShipTemplate {
    private final String id;
    private final String name;
    private final ShipHull hull;
    private final List<ShipPart> parts;
    private final PartSelection selection;

    public ShipTemplate(String id, String name, ShipHull hull, List<ShipPart> parts, PartSelection selection) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("template id is blank");
        this.id = id;
        this.name = name == null || name.isBlank() ? id : name;
        this.hull = hull == null ? ShipHull.empty() : hull;
        this.parts = parts == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(parts));
        this.selection = selection == null ? PartSelection.empty() : selection;
    }

    public String id() { return id; }
    public String name() { return name; }
    public ShipHull hull() { return hull; }
    public List<ShipPart> parts() { return parts; }
    public PartSelection selection() { return selection; }

    public ShipPart part(String partId) {
        return parts.stream().filter(part -> part.id().equals(partId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown part: " + partId));
    }

    public double maxSpeed() {
        double speed = 0.35;
        for (ShipPart part : parts) {
            double value = part.speed();
            String variant = selection.variant(part.id());
            if (!variant.isBlank()) value = Math.max(value, part.speed());
            speed = Math.max(speed, value);
        }
        return speed;
    }

    public ShipTemplate withHull(ShipHull next) { return new ShipTemplate(id, name, next, parts, selection); }
    public ShipTemplate withParts(List<ShipPart> next) { return new ShipTemplate(id, name, hull, next, selection); }
    public ShipTemplate withSelection(PartSelection next) { return new ShipTemplate(id, name, hull, parts, next); }
    public ShipTemplate withName(String next) { return new ShipTemplate(id, next, hull, parts, selection); }
    public ShipTemplate withId(String next) { return new ShipTemplate(next, name, hull, parts, selection); }

    @Override public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof ShipTemplate template)) return false;
        return id.equals(template.id) && name.equals(template.name) && hull.equals(template.hull)
                && parts.equals(template.parts) && selection.equals(template.selection);
    }

    @Override public int hashCode() { return Objects.hash(id, name, hull, parts, selection); }
}
