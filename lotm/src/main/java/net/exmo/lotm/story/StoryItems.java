package net.exmo.lotm.story;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Past-line supplies, limited claims, unique books, and the future-line sealed key.
 * Chest refresh, NPC claims, godfather/veteran grants, and the priest's next-cycle book are not wired yet.
 */
public final class StoryItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("lotm");
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "lotm");

    public static final DeferredItem<MessageTokenItem> MESSAGE_TOKEN = ITEMS.register("message_token",
            () -> new MessageTokenItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<StoryKnifeItem> KNIFE = ITEMS.register("knife",
            () -> new StoryKnifeItem(knife(), "item.lotm.knife.desc"));
    public static final DeferredItem<StoryKnifeItem> TABLE_KNIFE = ITEMS.register("table_knife",
            () -> new StoryKnifeItem(knife(), "item.lotm.table_knife.desc"));
    public static final DeferredItem<StoryCoinItem> COIN = ITEMS.register("coin",
            () -> new StoryCoinItem(new Item.Properties()));
    public static final DeferredItem<SpeakerItem> SPEAKER = ITEMS.register("speaker",
            () -> new SpeakerItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<PistolItem> PISTOL = ITEMS.register("pistol",
            () -> new PistolItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<StoryArmorItem> HELMET = ITEMS.register("basic_helmet",
            () -> new StoryArmorItem(ArmorItem.Type.HELMET, armor()));
    public static final DeferredItem<StoryArmorItem> CHESTPLATE = ITEMS.register("basic_chestplate",
            () -> new StoryArmorItem(ArmorItem.Type.CHESTPLATE, armor()));
    public static final DeferredItem<StoryArmorItem> LEGGINGS = ITEMS.register("basic_leggings",
            () -> new StoryArmorItem(ArmorItem.Type.LEGGINGS, armor()));
    public static final DeferredItem<StoryArmorItem> BOOTS = ITEMS.register("basic_boots",
            () -> new StoryArmorItem(ArmorItem.Type.BOOTS, armor()));
    public static final DeferredItem<RemedyItem> REMEDY = ITEMS.register("remedy",
            () -> new RemedyItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<StoryBookItem> RITUAL_BOOK = ITEMS.register("ritual_book",
            () -> new StoryBookItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), "item.lotm.ritual_book.pages"));
    public static final DeferredItem<StoryBookItem> CULTIST_ECHO = ITEMS.register("cultist_echo",
            () -> new StoryBookItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), "item.lotm.cultist_echo.pages"));
    public static final DeferredItem<SealedKeyItem> SEALED_KEY = ITEMS.register("sealed_key",
            () -> new SealedKeyItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> STORY_TAB = TABS.register("story", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.lotm.story"))
            .icon(() -> SEALED_KEY.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(MESSAGE_TOKEN.get());
                output.accept(KNIFE.get());
                output.accept(TABLE_KNIFE.get());
                output.accept(COIN.get());
                output.accept(SPEAKER.get());
                output.accept(PISTOL.get());
                output.accept(HELMET.get());
                output.accept(CHESTPLATE.get());
                output.accept(LEGGINGS.get());
                output.accept(BOOTS.get());
                output.accept(REMEDY.get());
                output.accept(RITUAL_BOOK.get());
                output.accept(CULTIST_ECHO.get());
                output.accept(SEALED_KEY.get());
            })
            .build());

    private StoryItems() {}

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        TABS.register(bus);
    }

    private static Item.Properties knife() {
        return new Item.Properties().attributes(ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 3.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -2.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build());
    }

    private static Item.Properties armor() {
        return new Item.Properties().rarity(Rarity.UNCOMMON);
    }
}
