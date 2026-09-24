package net.exmo.lotm.sequence;

import net.minecraft.resources.ResourceLocation;

/**
 * A skill printed on one sequence.
 * Active skills reference an Iron's Spells spell id. Passive skills reference a registered passive.
 */
public record SequenceSkill(
        SkillKind kind,
        ResourceLocation ref,
        int level,
        String nameKey,
        String descriptionKey,
        String iconItem
) {
    public SequenceSkill {
        if (kind == null) throw new IllegalArgumentException("kind");
        if (ref == null) throw new IllegalArgumentException("ref");
        if (level < 1) level = 1;
        if (nameKey == null) nameKey = "";
        if (descriptionKey == null) descriptionKey = "";
        if (iconItem == null) iconItem = "";
    }

    public static SequenceSkill active(ResourceLocation spellId, int level, String nameKey, String descriptionKey) {
        return new SequenceSkill(SkillKind.ACTIVE, spellId, level, nameKey, descriptionKey, "");
    }

    public static SequenceSkill passive(ResourceLocation passiveId, String nameKey, String descriptionKey, String iconItem) {
        return new SequenceSkill(SkillKind.PASSIVE, passiveId, 1, nameKey, descriptionKey, iconItem);
    }
}
