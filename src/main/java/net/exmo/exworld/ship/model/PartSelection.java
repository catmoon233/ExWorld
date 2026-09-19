package net.exmo.exworld.ship.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Current variant id per 飞船部位. Missing or blank values keep the template's base voxels. */
public final class PartSelection {
    private final Map<String, String> variantByPart;

    public PartSelection(Map<String, String> variantByPart) {
        LinkedHashMap<String, String> copy = new LinkedHashMap<>();
        if (variantByPart != null) {
            variantByPart.forEach((part, variant) -> {
                if (part != null && !part.isBlank() && variant != null && !variant.isBlank()) copy.put(part, variant);
            });
        }
        this.variantByPart = Collections.unmodifiableMap(copy);
    }

    public static PartSelection empty() { return new PartSelection(Map.of()); }

    public Map<String, String> variantByPart() { return variantByPart; }
    public String variant(String partId) { return variantByPart.getOrDefault(partId, ""); }
    public boolean isBase(String partId) { return !variantByPart.containsKey(partId); }

    public PartSelection with(String partId, String variantId) {
        LinkedHashMap<String, String> next = new LinkedHashMap<>(variantByPart);
        if (variantId == null || variantId.isBlank()) next.remove(partId);
        else next.put(partId, variantId);
        return new PartSelection(next);
    }

    @Override public boolean equals(Object object) {
        return object instanceof PartSelection selection && variantByPart.equals(selection.variantByPart);
    }

    @Override public int hashCode() { return Objects.hash(variantByPart); }
}
