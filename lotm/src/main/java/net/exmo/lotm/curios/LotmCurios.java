package net.exmo.lotm.curios;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;

import java.util.List;

public final class LotmCurios {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("lotm");
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "lotm");

    public static final DeferredItem<LotmCurio> HOLY_SPIRIT_CROSS = item("holy_spirit_cross", Rarity.RARE, "necklace",
            List.of(), null, LotmCurio.gold(), properties -> properties.durability(2).fireResistant());
    public static final DeferredItem<LotmCurio> IRON_AMULET = item("iron_amulet", Rarity.COMMON, "charm",
            List.of(bonus("armor", Attributes.ARMOR, 10.0, AttributeModifier.Operation.ADD_VALUE)),
            null, LotmCurio.iron(), null);
    public static final DeferredItem<LotmCurio> SIMPLE_WARD_AMULET = item("simple_ward_amulet", Rarity.COMMON, "charm",
            List.of(bonus("armor", Attributes.ARMOR, 6.0, AttributeModifier.Operation.ADD_VALUE)),
            null, LotmCurio.iron(), null);
    public static final DeferredItem<LotmCurio> MOON_DEW_RING = item("moon_dew_ring", Rarity.UNCOMMON, "ring",
            List.of(), CurioBehaviors::moonDew, LotmCurio.gold(), null);
    public static final DeferredItem<LotmCurio> FOOL_SILVER_COIN = item("fool_silver_coin", Rarity.UNCOMMON, "charm",
            List.of(bonus("luck", Attributes.LUCK, 1.0, AttributeModifier.Operation.ADD_VALUE)),
            null, LotmCurio.gold(), null);
    public static final DeferredItem<LotmCurio> SEER_MONOCLE = item("seer_monocle", Rarity.RARE, "head",
            List.of(bonus("spell_power", AttributeRegistry.SPELL_POWER, 0.05, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)),
            CurioBehaviors::seerSight, LotmCurio.gold(), null);
    public static final DeferredItem<LotmCurio> SLEEPLESS_EARRING = item("sleepless_earring", Rarity.UNCOMMON, "head",
            List.of(bonus("speed", Attributes.MOVEMENT_SPEED, 0.08, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)),
            CurioBehaviors::sleepless, LotmCurio.chain(), null);
    public static final DeferredItem<LotmCurio> SAILOR_COMPASS = item("sailor_compass", Rarity.UNCOMMON, "belt",
            List.of(
                    bonus("water", Attributes.WATER_MOVEMENT_EFFICIENCY, 0.25, AttributeModifier.Operation.ADD_VALUE),
                    bonus("oxygen", Attributes.OXYGEN_BONUS, 4.0, AttributeModifier.Operation.ADD_VALUE)),
            CurioBehaviors::sailor, LotmCurio.chain(), null);
    public static final DeferredItem<LotmCurio> RED_PRIEST_BRAND = item("red_priest_brand", Rarity.RARE, "bracelet",
            List.of(bonus("attack", Attributes.ATTACK_DAMAGE, 1.0, AttributeModifier.Operation.ADD_VALUE)),
            null, LotmCurio.iron(), null);
    public static final DeferredItem<LotmCurio> THIEF_GLOVES = item("thief_gloves", Rarity.UNCOMMON, "hands",
            List.of(
                    bonus("attack_speed", Attributes.ATTACK_SPEED, 0.10, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                    bonus("reach", Attributes.BLOCK_INTERACTION_RANGE, 0.5, AttributeModifier.Operation.ADD_VALUE)),
            null, LotmCurio.leather(), null);
    public static final DeferredItem<LotmCurio> CONCEALMENT_CLOAK = item("concealment_cloak", Rarity.RARE, "body",
            List.of(), CurioBehaviors::conceal, LotmCurio.leather(), null);
    public static final DeferredItem<LotmCurio> DUSK_MANTLE = item("dusk_mantle", Rarity.UNCOMMON, "back",
            List.of(
                    bonus("knockback", Attributes.KNOCKBACK_RESISTANCE, 0.2, AttributeModifier.Operation.ADD_VALUE),
                    bonus("toughness", Attributes.ARMOR_TOUGHNESS, 2.0, AttributeModifier.Operation.ADD_VALUE)),
            null, LotmCurio.leather(), null);
    public static final DeferredItem<LotmCurio> DEATH_KNELL_PENDANT = item("death_knell_pendant", Rarity.RARE, "necklace",
            List.of(), null, LotmCurio.chain(), null);
    public static final DeferredItem<LotmCurio> FATE_RING = item("fate_ring", Rarity.RARE, "ring",
            List.of(bonus("health", Attributes.MAX_HEALTH, 4.0, AttributeModifier.Operation.ADD_VALUE)),
            CurioBehaviors::fatePulse, LotmCurio.gold(), null);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CURIOS_TAB = TABS.register("curios", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.lotm.curios"))
            .icon(() -> HOLY_SPIRIT_CROSS.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(HOLY_SPIRIT_CROSS.get());
                output.accept(IRON_AMULET.get());
                output.accept(SIMPLE_WARD_AMULET.get());
                output.accept(MOON_DEW_RING.get());
                output.accept(FOOL_SILVER_COIN.get());
                output.accept(SEER_MONOCLE.get());
                output.accept(SLEEPLESS_EARRING.get());
                output.accept(SAILOR_COMPASS.get());
                output.accept(RED_PRIEST_BRAND.get());
                output.accept(THIEF_GLOVES.get());
                output.accept(CONCEALMENT_CLOAK.get());
                output.accept(DUSK_MANTLE.get());
                output.accept(DEATH_KNELL_PENDANT.get());
                output.accept(FATE_RING.get());
            })
            .build());

    private LotmCurios() {}

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        TABS.register(bus);
    }

    private static LotmCurio.Bonus bonus(String name, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
                                         double amount, AttributeModifier.Operation operation) {
        return new LotmCurio.Bonus(name, attribute, amount, operation);
    }

    private static DeferredItem<LotmCurio> item(String id, Rarity rarity, String slot, List<LotmCurio.Bonus> bonuses,
                                                LotmCurio.Ticker ticker, net.minecraft.sounds.SoundEvent sound,
                                                java.util.function.Consumer<Item.Properties> extra) {
        return ITEMS.register(id, () -> {
            Item.Properties properties = new Item.Properties().stacksTo(1).rarity(rarity);
            if (extra != null) extra.accept(properties);
            return new LotmCurio(properties, slot, "item.lotm." + id + ".desc", bonuses, ticker, sound);
        });
    }
}
