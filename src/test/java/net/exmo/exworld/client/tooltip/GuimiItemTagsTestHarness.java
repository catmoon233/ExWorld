package net.exmo.exworld.client.tooltip;

import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

public final class GuimiItemTagsTestHarness {
    public static void main(String[] args) {
        List<NameTag> seer = GuimiItemTags.labels(
                GuimiItemTags.classify("guimi_mod", "seer_potion", OptionalInt.empty()),
                key -> key);
        check(seer.size() == 2, "seer potion has two tags");
        check("魔药".equals(seer.get(0).label()), "seer is a beyonder potion, got " + seer.get(0).label());
        check("序列9".equals(seer.get(1).label()), "seer is sequence 9, got " + seer.get(1).label());

        List<NameTag> scroll = GuimiItemTags.labels(
                GuimiItemTags.classify("guimi_mod", "recipe_scroll_clown_potion", OptionalInt.empty()),
                key -> key);
        check(scroll.size() == 2, "recipe scroll has two tags");
        check("配方".equals(scroll.get(0).label()), "scroll is a recipe");
        check("序列8".equals(scroll.get(1).label()), "clown scroll is sequence 8, got " + scroll.get(1).label());

        List<NameTag> numbered = GuimiItemTags.labels(
                GuimiItemTags.classify("guimi_mod", "door_6_potion", OptionalInt.empty()),
                key -> key);
        check("魔药".equals(numbered.get(0).label()) && "序列6".equals(numbered.get(1).label()), "door 6");

        List<NameTag> emperor = GuimiItemTags.labels(
                GuimiItemTags.classify("guimi_mod", "recipe_scroll_black_emperor_9_potion", OptionalInt.empty()),
                key -> key);
        check("配方".equals(emperor.get(0).label()) && "序列9".equals(emperor.get(1).label()), "emperor scroll");

        check(GuimiItemTags.classify("minecraft", "potion", OptionalInt.empty()).empty(), "vanilla potion is not guimi");
        check(GuimiItemTags.classify("guimi_mod", "wand", OptionalInt.empty()).empty(), "wand has no sequence tag");

        List<NameTag> known = GuimiItemTags.labels(
                GuimiItemTags.classify("guimi_mod", "characteristic", OptionalInt.of(5)),
                key -> "tooltip.exworld.tag.sequence".equals(key) ? "Seq " : key);
        check(known.size() == 1 && "Seq 5".equals(known.getFirst().label()), "reflected sequence uses translation");
        check(GuimiItemTags.sequenceColor(0) != GuimiItemTags.sequenceColor(9), "sequence colours differ");
        check(GuimiItemTags.sequenceOf("guimi_mod:fool_9").orElse(-1) == 9, "sequence id path");

        Map<String, Integer> materials = GuimiItemTags.uniqueMaterialSequences(Map.of(
                "seer_potion", List.of("lava_octopus_blood", "poison_hemlock"),
                "clown_potion", List.of("face_rose", "poison_hemlock"),
                "door_6_potion", List.of("door_crystal")
        ));
        check(Integer.valueOf(9).equals(materials.get("lava_octopus_blood")), "unique seer material");
        check(Integer.valueOf(8).equals(materials.get("face_rose")), "unique clown material");
        check(Integer.valueOf(6).equals(materials.get("door_crystal")), "numbered recipe material");
        check(!materials.containsKey("poison_hemlock"), "shared material is not pinned to one sequence");

        System.out.println("Guimi item tag tests passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
