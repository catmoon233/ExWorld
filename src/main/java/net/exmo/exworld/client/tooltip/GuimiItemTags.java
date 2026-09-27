package net.exmo.exworld.client.tooltip;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.function.Function;

/**
 * Name tags for guimi_mod potions, recipe scrolls, and other sequence-bound items.
 * Sequence numbers come from the item id, {@code targetSequenceId}, characteristic components,
 * or a brewing ingredient used by only one sequence.
 */
public final class GuimiItemTags {
    public static final String NAMESPACE = "guimi_mod";
    public static final int POTION_COLOR = 0xFF9D62CA;
    public static final int RECIPE_COLOR = 0xFFE2A834;

    private static final String RECIPE_PREFIX = "recipe_scroll_";
    private static final String POTION_SUFFIX = "_potion";
    private static volatile Map<Item, Integer> MATERIAL_SEQUENCES;

    /** Named potions whose ids do not end in the sequence digit. */
    private static final Map<String, Integer> NAMED_POTIONS = Map.ofEntries(
            Map.entry("seer_potion", 9),
            Map.entry("clown_potion", 8),
            Map.entry("magician_potion", 7),
            Map.entry("faceless_potion", 6),
            Map.entry("marionettist_potion", 5),
            Map.entry("assassin_potion", 9),
            Map.entry("instigator_potion", 8),
            Map.entry("witch_potion", 7),
            Map.entry("joyful_witch_potion", 6),
            Map.entry("hunter_potion", 9),
            Map.entry("provoker_potion", 8),
            Map.entry("pyromaniac_potion", 7),
            Map.entry("conspirer_potion", 6),
            Map.entry("mystic_prayer_potion", 9),
            Map.entry("listener_potion", 8),
            Map.entry("hermit_potion", 7),
            Map.entry("rose_bishop_potion", 6),
            Map.entry("spectator_potion", 9),
            Map.entry("mind_reader_potion", 8),
            Map.entry("psychologist_potion", 7),
            Map.entry("hypnotist_potion", 6),
            Map.entry("sailor_potion", 9),
            Map.entry("wrathful_potion", 8),
            Map.entry("navigator_potion", 7),
            Map.entry("wind_favored_potion", 6),
            Map.entry("praiser_potion", 9),
            Map.entry("light_seeker_potion", 8),
            Map.entry("sun_priest_potion", 7),
            Map.entry("notary_potion", 6),
            Map.entry("reader_potion", 9),
            Map.entry("reasoning_student_potion", 8),
            Map.entry("knowledge_guardian_potion", 7),
            Map.entry("erudite_potion", 6),
            Map.entry("warrior_potion", 9),
            Map.entry("fighter_potion", 8),
            Map.entry("weapon_master_potion", 7),
            Map.entry("dawn_knight_potion", 6),
            Map.entry("sleepless_potion", 9),
            Map.entry("midnight_poet_potion", 8),
            Map.entry("nightmare_potion", 7),
            Map.entry("requiem_potion", 6),
            Map.entry("corpse_collector_potion", 9),
            Map.entry("gravedigger_potion", 8),
            Map.entry("spirit_medium_potion", 7),
            Map.entry("necromancer_potion", 6)
    );

    private GuimiItemTags() {}

    public record Kind(boolean potion, boolean recipe, OptionalInt sequence) {
        public static final Kind NONE = new Kind(false, false, OptionalInt.empty());

        public boolean empty() {
            return !potion && !recipe && sequence.isEmpty();
        }
    }

    public static List<NameTag> tags(ItemStack stack, Function<String, String> translate) {
        if (stack == null || stack.isEmpty()) return List.of();
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) return List.of();
        Kind kind = classify(id.getNamespace(), id.getPath(), sequenceFrom(stack));
        if (kind.empty()) {
            OptionalInt traced = tracedSequence(stack.getItem());
            if (traced.isEmpty()) return List.of();
            kind = new Kind(false, false, traced);
        }
        return labels(kind, translate);
    }

    /**
     * Ingredient path to the only sequence whose brewing recipe uses it.
     * Shared ingredients are omitted: they cannot be traced to one sequence.
     */
    public static Map<String, Integer> uniqueMaterialSequences(Map<String, ? extends Iterable<String>> recipeIngredients) {
        Map<String, java.util.Set<Integer>> found = new java.util.LinkedHashMap<>();
        if (recipeIngredients == null) return Map.of();
        for (Map.Entry<String, ? extends Iterable<String>> recipe : recipeIngredients.entrySet()) {
            OptionalInt sequence = sequenceOf(recipe.getKey());
            if (sequence.isEmpty() || recipe.getValue() == null) continue;
            for (String ingredient : recipe.getValue()) {
                if (ingredient == null || ingredient.isBlank()) continue;
                found.computeIfAbsent(ingredient, key -> new java.util.LinkedHashSet<>()).add(sequence.getAsInt());
            }
        }
        Map<String, Integer> unique = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, java.util.Set<Integer>> entry : found.entrySet()) {
            if (entry.getValue().size() == 1) unique.put(entry.getKey(), entry.getValue().iterator().next());
        }
        return unique;
    }

    public static Kind classify(String namespace, String path, OptionalInt knownSequence) {
        if (!NAMESPACE.equals(namespace) || path == null || path.isBlank()) return Kind.NONE;
        boolean recipe = path.startsWith(RECIPE_PREFIX);
        String body = recipe ? path.substring(RECIPE_PREFIX.length()) : path;
        boolean potion = !recipe && body.endsWith(POTION_SUFFIX);
        OptionalInt sequence = knownSequence.isPresent() ? knownSequence : sequenceOf(body);
        if (!recipe && !potion && sequence.isEmpty()) return Kind.NONE;
        return new Kind(potion, recipe, sequence);
    }

    public static OptionalInt sequenceOf(String path) {
        if (path == null || path.isBlank()) return OptionalInt.empty();
        String body = path.startsWith(RECIPE_PREFIX) ? path.substring(RECIPE_PREFIX.length()) : path;
        Integer named = NAMED_POTIONS.get(body);
        if (named != null) return OptionalInt.of(named);
        if (!body.endsWith(POTION_SUFFIX)) {
            named = NAMED_POTIONS.get(body + POTION_SUFFIX);
            if (named != null) return OptionalInt.of(named);
        }
        String stem = body.endsWith(POTION_SUFFIX) ? body.substring(0, body.length() - POTION_SUFFIX.length()) : body;
        int mark = stem.lastIndexOf('_');
        if (mark >= 0 && mark == stem.length() - 2) {
            char digit = stem.charAt(stem.length() - 1);
            if (digit >= '0' && digit <= '9') return OptionalInt.of(digit - '0');
        }
        return OptionalInt.empty();
    }

    public static int sequenceColor(int level) {
        int clamped = Math.max(0, Math.min(9, level));
        return RarityPalette.lerp(0xFFE8C36A, 0xFF7DCEA0, clamped / 9.0f);
    }

    public static List<NameTag> labels(Kind kind, Function<String, String> translate) {
        if (kind == null || kind.empty()) return List.of();
        Function<String, String> text = translate == null ? key -> key : translate;
        List<NameTag> tags = new ArrayList<>(2);
        if (kind.recipe()) {
            tags.add(new NameTag(fallback(text, "tooltip.exworld.tag.recipe", "配方"), RECIPE_COLOR));
        } else if (kind.potion()) {
            tags.add(new NameTag(fallback(text, "tooltip.exworld.tag.beyonder_potion", "魔药"), POTION_COLOR));
        }
        if (kind.sequence().isPresent()) {
            int level = kind.sequence().getAsInt();
            tags.add(new NameTag(fallback(text, "tooltip.exworld.tag.sequence", "序列") + level, sequenceColor(level)));
        }
        return List.copyOf(tags);
    }

    static OptionalInt sequenceFrom(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return OptionalInt.empty();
        OptionalInt fromItem = sequenceFromItem(stack.getItem());
        if (fromItem.isPresent()) return fromItem;
        return sequenceFromComponents(stack);
    }

    private static OptionalInt sequenceFromItem(Item item) {
        if (item == null) return OptionalInt.empty();
        for (Class<?> type = item.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            OptionalInt sequence = readSequenceField(item, type);
            if (sequence.isPresent()) return sequence;
            String potionId = readStringField(item, type, "potionId");
            if (potionId != null) return sequenceOf(potionId);
        }
        return OptionalInt.empty();
    }

    private static OptionalInt readSequenceField(Item item, Class<?> type) {
        try {
            Field field = type.getDeclaredField("targetSequenceId");
            field.setAccessible(true);
            Object value = field.get(item);
            if (value == null) return OptionalInt.empty();
            return sequenceOf(value.toString());
        } catch (NoSuchFieldException ignored) {
            return OptionalInt.empty();
        } catch (Throwable ignored) {
            return OptionalInt.empty();
        }
    }

    private static String readStringField(Item item, Class<?> type, String name) {
        try {
            Field field = type.getDeclaredField(name);
            field.setAccessible(true);
            Object value = field.get(item);
            return value instanceof String text && !text.isBlank() ? text : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static OptionalInt sequenceFromComponents(ItemStack stack) {
        for (String path : List.of("characteristic", "sealed_artifact", "magic_artifact")) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(NAMESPACE, path);
            if (!BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(id)) continue;
            DataComponentType<?> type = BuiltInRegistries.DATA_COMPONENT_TYPE.get(id);
            if (type == null || !stack.has(type)) continue;
            Object data = stack.get(type);
            if (data == null) continue;
            try {
                Object level = data.getClass().getMethod("level").invoke(data);
                if (level instanceof Integer value && value >= 0 && value <= 9) return OptionalInt.of(value);
            } catch (Throwable ignored) {
                // Component shape changed; the id-based sequence still applies to potions.
            }
        }
        return OptionalInt.empty();
    }

    private static OptionalInt tracedSequence(Item item) {
        if (item == null) return OptionalInt.empty();
        Integer sequence = materialIndex().get(item);
        return sequence == null ? OptionalInt.empty() : OptionalInt.of(sequence);
    }

    private static Map<Item, Integer> materialIndex() {
        Map<Item, Integer> cached = MATERIAL_SEQUENCES;
        if (cached != null) return cached;
        Map<Item, Integer> built = buildMaterialIndex();
        if (built.isEmpty()) return Map.of();
        MATERIAL_SEQUENCES = built;
        return built;
    }

    @SuppressWarnings("unchecked")
    private static Map<Item, Integer> buildMaterialIndex() {
        try {
            Class<?> manager = Class.forName("com.wan.gmmod.content.brewing.BrewingRecipeManager");
            Field field = manager.getDeclaredField("RECIPES");
            field.setAccessible(true);
            Object value = field.get(null);
            if (!(value instanceof List<?> recipes) || recipes.isEmpty()) return Map.of();
            Map<Item, java.util.Set<Integer>> found = new java.util.HashMap<>();
            for (Object recipe : recipes) {
                if (recipe == null) continue;
                Object id = recipe.getClass().getMethod("id").invoke(recipe);
                OptionalInt sequence = sequenceOf(id == null ? "" : id.toString());
                if (sequence.isEmpty()) continue;
                Object ingredients = recipe.getClass().getMethod("ingredients").invoke(recipe);
                if (!(ingredients instanceof Map<?, ?> map)) continue;
                for (Object key : map.keySet()) {
                    if (!(key instanceof Item ingredient)) continue;
                    ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(ingredient);
                    if (itemId == null || !NAMESPACE.equals(itemId.getNamespace())) continue;
                    found.computeIfAbsent(ingredient, ignored -> new java.util.LinkedHashSet<>()).add(sequence.getAsInt());
                }
            }
            Map<Item, Integer> unique = new java.util.HashMap<>();
            for (Map.Entry<Item, java.util.Set<Integer>> entry : found.entrySet()) {
                if (entry.getValue().size() == 1) unique.put(entry.getKey(), entry.getValue().iterator().next());
            }
            return unique;
        } catch (Throwable ignored) {
            return Map.of();
        }
    }

    private static String fallback(Function<String, String> translate, String key, String fallback) {
        String value = translate.apply(key);
        return value == null || value.isBlank() || value.equals(key) ? fallback : value;
    }
}
