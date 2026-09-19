package net.exmo.exworld.client.tooltip;

import net.exmo.exmodifier.api.AppliedModifierView;
import net.exmo.exmodifier.api.ExModifierApi;
import net.exmo.exmodifier.core.registry.ExModifierCatalog;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/** Builds a {@link TooltipModel} from a stack, vanilla lines, and the player's equipped modifiers. */
public final class TooltipModelFactory {
    private static final int TAG_WEAPON = 0xFFDB5E71;
    private static final int TAG_TOOL = 0xFF5E8ACF;
    private static final int TAG_ARMOR = 0xFFE2A834;
    private static final int TAG_ITEM = 0xFF8A8A90;

    private TooltipModelFactory() {}

    public static TooltipModel build(ItemStack stack, List<Component> rawLines, LivingEntity wearer) {
        ItemStack item = stack == null ? ItemStack.EMPTY : stack;
        Function<String, String> translate = TooltipModelFactory::translate;
        List<NameTag> tags = new ArrayList<>();
        categoryTag(item).ifPresent(tags::add);

        ExModifierCatalog catalog = ExModifierCatalog.current();
        Optional<net.minecraft.resources.ResourceLocation> quality = ExModifierApi.qualityOn(item);
        ExModifierTooltip.qualityTag(quality, catalog, translate).ifPresent(tags::add);

        List<AppliedModifierView> onItem = ExModifierApi.modifiersOn(item);
        List<AppliedModifierView> equipped = wearer == null ? onItem : ExModifierApi.modifiersOnEquipped(wearer);
        List<Chip> chips = new ArrayList<>(ExModifierTooltip.chips(onItem, catalog, translate));
        chips.addAll(ExModifierTooltip.elementChips(ExModifierApi.elementsOn(item), translate));

        List<VanillaLineFilter.Line> lines = new ArrayList<>();
        if (rawLines != null) {
            for (Component component : rawLines) lines.add(from(component));
        }
        List<String> body = VanillaLineFilter.body(lines).stream().map(VanillaLineFilter.Line::text).toList();

        Rarity rarity = item.isEmpty() ? Rarity.COMMON : item.getRarity();
        return new TooltipModel(
                item.isEmpty() ? "" : item.getHoverName().getString(),
                tags,
                translate.apply(rarityKey(rarity)),
                rarity,
                quality,
                chips,
                ExModifierTooltip.suits(onItem, equipped, catalog, translate),
                body
        );
    }

    public static VanillaLineFilter.Line from(Component component) {
        if (component == null) return new VanillaLineFilter.Line("", "");
        String key = "";
        if (component.getContents() instanceof TranslatableContents translatable) {
            key = translatable.getKey();
        }
        return new VanillaLineFilter.Line(key, component.getString());
    }

    private static Optional<NameTag> categoryTag(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return Optional.empty();
        Item item = stack.getItem();
        if (item instanceof SwordItem) return Optional.of(tag("sword", TAG_WEAPON));
        if (item instanceof AxeItem) return Optional.of(tag("axe", TAG_WEAPON));
        if (item instanceof BowItem || item instanceof CrossbowItem) return Optional.of(tag("bow", TAG_WEAPON));
        if (item instanceof PickaxeItem) return Optional.of(tag("pickaxe", TAG_TOOL));
        if (item instanceof ShovelItem) return Optional.of(tag("shovel", TAG_TOOL));
        if (item instanceof HoeItem) return Optional.of(tag("hoe", TAG_TOOL));
        if (item instanceof ShieldItem) return Optional.of(tag("shield", TAG_ARMOR));
        if (item instanceof ArmorItem armor) {
            return Optional.of(switch (armor.getType()) {
                case HELMET -> tag("helmet", TAG_ARMOR);
                case CHESTPLATE -> tag("chestplate", TAG_ARMOR);
                case LEGGINGS -> tag("leggings", TAG_ARMOR);
                case BOOTS -> tag("boots", TAG_ARMOR);
                default -> tag("armor", TAG_ARMOR);
            });
        }
        if (item instanceof PotionItem || stack.has(DataComponents.POTION_CONTENTS)) {
            return Optional.of(tag("potion", 0xFF9D62CA));
        }
        if (stack.has(DataComponents.FOOD)) return Optional.of(tag("food", 0xFF6FCB63));
        if (item instanceof BlockItem) return Optional.of(tag("block", TAG_ITEM));
        return Optional.of(tag("item", TAG_ITEM));
    }

    private static NameTag tag(String path, int color) {
        return new NameTag(translate("tooltip.exworld.tag." + path), color);
    }

    private static String rarityKey(Rarity rarity) {
        if (rarity == null) return "tooltip.exworld.rarity.common";
        return switch (rarity) {
            case UNCOMMON -> "tooltip.exworld.rarity.uncommon";
            case RARE -> "tooltip.exworld.rarity.rare";
            case EPIC -> "tooltip.exworld.rarity.epic";
            default -> "tooltip.exworld.rarity.common";
        };
    }

    private static String translate(String key) {
        if (key == null || key.isBlank()) return "";
        try {
            if (I18n.exists(key)) return I18n.get(key);
        } catch (Throwable ignored) {
            // Tests and early load have no language instance.
        }
        return key;
    }
}
