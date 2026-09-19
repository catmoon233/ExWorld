package net.exmo.exworld.client.tooltip;

import net.exmo.exmodifier.api.AppliedModifierView;
import net.exmo.exmodifier.api.ExModifierApi;
import net.exmo.exmodifier.core.data.AttributeSpec;
import net.exmo.exmodifier.core.data.ModifierEntryDefinition;
import net.exmo.exmodifier.core.data.QualityDefinition;
import net.exmo.exmodifier.core.data.SuitDefinition;
import net.exmo.exmodifier.core.data.SuitLevel;
import net.exmo.exmodifier.core.registry.ExModifierCatalog;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/** Turns ExModifier catalog data into chips and suit sections for the themed tooltip. */
public final class ExModifierTooltip {
    private static final int[] SLOT_COLORS = {0xFFDB5E71, 0xFFE2A834, 0xFF5E8ACF, 0xFF6FCB63};
    private static final int ELEMENT_COLOR = 0xFFFF8A4A;

    private ExModifierTooltip() {}

    public record SuitBonus(int pieces, String text, boolean active) {}

    public record SlotSection(int total, int unlocked, List<String> names) {
        public SlotSection {
            names = List.copyOf(names == null ? List.of() : names);
            total = Math.max(0, total);
            unlocked = Math.max(0, Math.min(total, unlocked));
        }
    }

    public record SuitSection(String name, int owned, int required, List<SuitBonus> bonuses) {
        public SuitSection {
            name = name == null ? "" : name;
            bonuses = List.copyOf(bonuses == null ? List.of() : bonuses);
            owned = Math.max(0, owned);
            required = Math.max(1, required);
        }
    }

    public static SlotSection slotSection(ItemStack stack, ExModifierCatalog catalog, Function<String, String> translate) {
        if (stack == null || stack.isEmpty()) return new SlotSection(0, 0, List.of());
        ExModifierCatalog source = catalog == null ? ExModifierCatalog.EMPTY : catalog;
        Function<String, String> tr = translate == null ? key -> key : translate;
        List<ResourceLocation> unlockedIds = ExModifierApi.unlockedSlots(stack).slots();
        int total = source.slots().size();
        if (total == 0 && unlockedIds.isEmpty()) return new SlotSection(0, 0, List.of());
        List<String> names = new ArrayList<>(unlockedIds.size());
        for (ResourceLocation id : unlockedIds) {
            String key = "tooltip.exmodifier.slot." + id.getPath();
            String name = tr.apply(key);
            if (name == null || name.isBlank() || name.equals(key)) name = pretty(id.getPath());
            names.add(name);
        }
        return new SlotSection(Math.max(total, unlockedIds.size()), unlockedIds.size(), names);
    }

    public static List<Chip> chips(
            List<AppliedModifierView> views,
            ExModifierCatalog catalog,
            Function<String, String> translate
    ) {
        if (views == null || views.isEmpty()) return List.of();
        ExModifierCatalog source = catalog == null ? ExModifierCatalog.EMPTY : catalog;
        Function<String, String> tr = translate == null ? key -> key : translate;
        List<Chip> chips = new ArrayList<>();
        for (AppliedModifierView view : views) {
            Optional<ModifierEntryDefinition> definition = source.entry(view.entryId());
            String key = definition.flatMap(ModifierEntryDefinition::descriptionKey)
                    .orElse("tooltip.exmodifier.entry." + view.entryId().getPath());
            String name = tr.apply(key);
            if (name == null || name.isBlank() || name.equals(key)) name = pretty(view.entryId().getPath());
            chips.add(new Chip(name + " Lv." + view.level(), colorFor(view.slotId())));
        }
        return List.copyOf(chips);
    }

    public static List<Chip> elementChips(
            java.util.Map<ResourceLocation, Integer> elements,
            Function<String, String> translate
    ) {
        if (elements == null || elements.isEmpty()) return List.of();
        Function<String, String> tr = translate == null ? key -> key : translate;
        List<Chip> chips = new ArrayList<>();
        elements.forEach((id, amount) -> {
            String key = "tooltip.exmodifier.element." + id.getPath();
            String name = tr.apply(key);
            if (name == null || name.isBlank() || name.equals(key)) name = pretty(id.getPath());
            chips.add(new Chip(name + " " + amount, ELEMENT_COLOR));
        });
        return List.copyOf(chips);
    }

    public static Optional<NameTag> qualityTag(
            Optional<ResourceLocation> qualityId,
            ExModifierCatalog catalog,
            Function<String, String> translate
    ) {
        if (qualityId == null || qualityId.isEmpty()) return Optional.empty();
        ResourceLocation id = qualityId.get();
        ExModifierCatalog source = catalog == null ? ExModifierCatalog.EMPTY : catalog;
        Function<String, String> tr = translate == null ? key -> key : translate;
        String key = source.quality(id).flatMap(QualityDefinition::tooltipKey)
                .orElse("tooltip.exmodifier.quality." + id.getPath());
        String name = tr.apply(key);
        if (name == null || name.isBlank() || name.equals(key)) name = pretty(id.getPath());
        return Optional.of(new NameTag(name, RarityPalette.qualityColor(id)));
    }

    public static List<SuitSection> suits(
            List<AppliedModifierView> itemModifiers,
            List<AppliedModifierView> equippedModifiers,
            ExModifierCatalog catalog,
            Function<String, String> translate
    ) {
        ExModifierCatalog source = catalog == null ? ExModifierCatalog.EMPTY : catalog;
        Function<String, String> tr = translate == null ? key -> key : translate;
        Set<ResourceLocation> suitIds = new LinkedHashSet<>();
        List<AppliedModifierView> onItem = itemModifiers == null ? List.of() : itemModifiers;
        for (AppliedModifierView view : onItem) {
            source.entry(view.entryId()).ifPresent(definition -> suitIds.addAll(definition.suits()));
            for (var entry : source.suits().entrySet()) {
                if (entry.getValue().entries().contains(view.entryId())) suitIds.add(entry.getKey());
            }
        }
        List<SuitSection> sections = new ArrayList<>();
        List<AppliedModifierView> equipped = equippedModifiers == null ? List.of() : equippedModifiers;
        for (ResourceLocation suitId : suitIds) {
            Optional<SuitDefinition> definition = source.suit(suitId);
            if (definition.isEmpty()) continue;
            SuitDefinition suit = definition.get();
            int owned = 0;
            for (AppliedModifierView view : equipped) {
                if (belongsToSuit(view.entryId(), suitId, suit, source)) owned++;
            }
            int required = 1;
            List<SuitBonus> bonuses = new ArrayList<>();
            for (SuitLevel level : suit.levels()) {
                required = Math.max(required, level.pieces());
                boolean active = owned >= level.pieces();
                String text = formatLevel(level, tr);
                bonuses.add(new SuitBonus(level.pieces(), text, active));
            }
            String key = "tooltip.exmodifier.suit." + suitId.getPath();
            String name = tr.apply(key);
            if (name == null || name.isBlank() || name.equals(key)) name = pretty(suitId.getPath());
            sections.add(new SuitSection(name, owned, required, bonuses));
        }
        return List.copyOf(sections);
    }

    public static String formatAttribute(AttributeSpec spec) {
        if (spec == null) return "";
        double amount = spec.amount();
        boolean percent = spec.operation() != AttributeModifier.Operation.ADD_VALUE;
        String number = formatNumber(percent ? amount * 100.0 : amount);
        if (percent) number += "%";
        String sign = amount >= 0 ? "+" : "";
        return sign + number + " " + pretty(spec.attribute().getPath());
    }

    private static String formatLevel(SuitLevel level, Function<String, String> translate) {
        StringBuilder text = new StringBuilder();
        for (AttributeSpec spec : level.attributes()) {
            if (!text.isEmpty()) text.append(", ");
            text.append(formatAttribute(spec));
        }
        if (text.isEmpty()) {
            String trigger = level.trigger() == null ? "" : pretty(level.trigger().getPath());
            if (!trigger.isBlank()) text.append(translate.apply(trigger));
        }
        return text.toString();
    }

    private static boolean belongsToSuit(
            ResourceLocation entryId,
            ResourceLocation suitId,
            SuitDefinition suit,
            ExModifierCatalog catalog
    ) {
        if (suit.entries().contains(entryId)) return true;
        return catalog.entry(entryId).map(definition -> definition.suits().contains(suitId)).orElse(false);
    }

    private static int colorFor(Optional<ResourceLocation> slotId) {
        if (slotId == null || slotId.isEmpty()) return SLOT_COLORS[0];
        int hash = Math.abs(slotId.get().getPath().hashCode());
        return SLOT_COLORS[hash % SLOT_COLORS.length];
    }

    static String pretty(String path) {
        if (path == null || path.isBlank()) return "";
        String trimmed = path;
        int dot = trimmed.lastIndexOf('.');
        if (dot >= 0 && dot < trimmed.length() - 1) trimmed = trimmed.substring(dot + 1);
        return trimmed.replace('_', ' ');
    }

    private static String formatNumber(double value) {
        if (Math.abs(value - Math.rint(value)) < 1.0e-6) return Long.toString(Math.round(value));
        String text = String.format(java.util.Locale.ROOT, "%.2f", value);
        if (text.indexOf('.') >= 0) {
            while (text.endsWith("0")) text = text.substring(0, text.length() - 1);
            if (text.endsWith(".")) text = text.substring(0, text.length() - 1);
        }
        return text;
    }
}
