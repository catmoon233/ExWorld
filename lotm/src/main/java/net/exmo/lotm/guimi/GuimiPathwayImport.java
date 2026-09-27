package net.exmo.lotm.guimi;

import com.wan.gmmod.content.abilities.Ability;
import com.wan.gmmod.content.abilities.AbilityRegistry;
import com.wan.gmmod.content.sequences.Sequence;
import com.wan.gmmod.content.sequences.Sequences;
import net.exmo.lotm.sequence.PathwayDefinition;
import net.exmo.lotm.sequence.SequenceRank;
import net.exmo.lotm.sequence.SequenceRegistry;
import net.exmo.lotm.sequence.SequenceSkill;
import net.exmo.lotm.sequence.SkillKind;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Fills pathway sheets upstream lotm does not define, and aliases guimi keys onto the canonical ids.
 * Existing upstream definitions are left intact.
 */
public final class GuimiPathwayImport {
    private GuimiPathwayImport() {}

    public static void install() {
        com.wan.gmmod.content.sequences.SequenceRegistry.init();
        for (Map.Entry<String, String> entry : GuimiPathways.canonicalToGuimi().entrySet()) {
            String canonical = entry.getKey();
            String guimi = entry.getValue();
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(GuimiPathways.NAMESPACE, canonical);
            alias(canonical, id);
            alias(GuimiPathways.NAMESPACE + ":" + canonical, id);
            alias("guimi_mod:" + canonical, id);
            alias(guimi, id);
            alias(GuimiPathways.NAMESPACE + ":" + guimi, id);
            alias("guimi_mod:" + guimi, id);
            Sequences.Pathway pathway = Sequences.fromKey(guimi);
            if (pathway == null) continue;
            if (SequenceRegistry.pathway(id).isEmpty()) {
                SequenceRegistry.register(definition(id, pathway));
            } else {
                merge(id, pathway);
            }
        }
    }


    private static void merge(ResourceLocation id, Sequences.Pathway pathway) {
        for (int level = Sequences.MAX_LEVEL; level >= 0; level--) {
            com.wan.gmmod.content.sequences.Sequence sequence =
                    com.wan.gmmod.content.sequences.SequenceRegistry.get(pathway, level);
            if (sequence == null) continue;
            SequenceRank rank = SequenceRank.parse(Integer.toString(level)).orElse(null);
            if (rank == null) continue;
            String name = sequence.getName() == null ? pathway.getDisplayName() : sequence.getName();
            String description = sequence.getDescription() == null ? "" : sequence.getDescription();
            SequenceRegistry.mergeSkills(id, rank, name, description, skills(sequence));
        }
    }
    private static PathwayDefinition definition(ResourceLocation id, Sequences.Pathway pathway) {
        PathwayDefinition.Builder builder = PathwayDefinition.builder(id, pathway.getDisplayName());
        for (int level = Sequences.MAX_LEVEL; level >= 0; level--) {
            Sequence sequence = com.wan.gmmod.content.sequences.SequenceRegistry.get(pathway, level);
            if (sequence == null) continue;
            SequenceRank rank = SequenceRank.parse(Integer.toString(level)).orElseThrow();
            String name = sequence.getName() == null ? pathway.getDisplayName() : sequence.getName();
            String description = sequence.getDescription() == null ? "" : sequence.getDescription();
            builder.sequence(rank, name, description, List.of(), skills(sequence));
        }
        builder.sequence(SequenceRank.OLD_ONE, "旧日", "sequence.guimi_mod.old_one.intro", List.of(), List.of());
        builder.sequence(SequenceRank.PILLAR, "支柱", "sequence.guimi_mod.pillar.intro", List.of(), List.of());
        return builder.build();
    }

    private static List<SequenceSkill> skills(Sequence sequence) {
        List<SequenceSkill> skills = new ArrayList<>();
        for (Ability ability : AbilityRegistry.getAbilitiesFor(sequence.getId())) {
            String icon = "tex:" + ability.getIconTexture();
            if (ability.isActive()) {
                skills.add(new SequenceSkill(SkillKind.ACTIVE, ability.getId(), 1,
                        ability.getNameKey(), ability.getDescriptionKey(), icon));
            } else {
                skills.add(SequenceSkill.passive(ability.getId(), ability.getNameKey(), ability.getDescriptionKey(), icon));
            }
        }
        return skills;
    }

    private static void alias(String token, ResourceLocation id) {
        SequenceRegistry.alias(token, id);
    }
}
