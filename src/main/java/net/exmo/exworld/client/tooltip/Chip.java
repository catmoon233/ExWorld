package net.exmo.exworld.client.tooltip;

/** Coloured chip drawn in wrapping rows (affixes, elements). */
public record Chip(String label, int color) {
    public Chip {
        label = label == null ? "" : label;
    }
}
