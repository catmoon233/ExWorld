package net.exmo.exworld.progress;

import net.minecraft.resources.ResourceLocation;

/** Content-only reward declaration. Action IDs are resolved against a fixed server registry. */
public record QuestReward(Kind kind, ResourceLocation id, int amount, String actionValue) {
    public enum Kind { RESOURCE, ITEM, EXPERIENCE, CARD, ACTION }
    public QuestReward {
        if (kind != Kind.EXPERIENCE && id == null) throw new IllegalArgumentException("reward id is required");
        if (amount < 0) throw new IllegalArgumentException("reward amount cannot be negative");
        actionValue = actionValue == null ? "" : actionValue;
    }
}
