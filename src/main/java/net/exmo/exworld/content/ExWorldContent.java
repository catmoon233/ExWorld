package net.exmo.exworld.content;

import net.exmo.exworld.Exworld;
import net.exmo.exworld.content.block.TravelAnchorBlock;
import net.exmo.exworld.content.block.DungeonEntranceBlock;
import net.exmo.exworld.content.block.DungeonEntranceBlockEntity;
import net.exmo.exworld.content.block.ShipCoreBlock;
import net.exmo.exworld.content.block.ShipHelmBlock;
import net.exmo.exworld.content.item.ShipToolItem;
import net.exmo.exworld.content.item.StorageCoreItem;
import net.exmo.exworld.inventory.InventoryRegistries;
import net.exmo.exworld.npc.entity.UrbanNpc;
import net.exmo.exworld.npc.item.NpcWandItem;
import net.exmo.exworld.ship.entity.ShipEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class ExWorldContent {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Exworld.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Exworld.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Exworld.MODID);
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Exworld.MODID);
    public static final DeferredBlock<Block> TRAVEL_ANCHOR = BLOCKS.register("travel_anchor", () -> new TravelAnchorBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(4.0F, 8.0F).lightLevel(state -> 8).sound(SoundType.METAL)));
    public static final DeferredItem<BlockItem> TRAVEL_ANCHOR_ITEM = ITEMS.register("travel_anchor",
            () -> new BlockItem(TRAVEL_ANCHOR.get(), new Item.Properties()));
    public static final DeferredBlock<Block> DUNGEON_ENTRANCE = BLOCKS.register("dungeon_entrance", () -> new DungeonEntranceBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(4.0F, 8.0F).lightLevel(state -> 10).sound(SoundType.AMETHYST)));
    public static final DeferredItem<BlockItem> DUNGEON_ENTRANCE_ITEM = ITEMS.register("dungeon_entrance",
            () -> new BlockItem(DUNGEON_ENTRANCE.get(), new Item.Properties()));
    public static final DeferredBlock<Block> SHIP_CORE = BLOCKS.register("ship_core", () -> new ShipCoreBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(3.0F, 6.0F).lightLevel(state -> 6).sound(SoundType.COPPER)));
    public static final DeferredItem<BlockItem> SHIP_CORE_ITEM = ITEMS.register("ship_core",
            () -> new BlockItem(SHIP_CORE.get(), new Item.Properties()));
    public static final DeferredBlock<Block> SHIP_HELM = BLOCKS.register("ship_helm", () -> new ShipHelmBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(3.0F, 6.0F).lightLevel(state -> 4).sound(SoundType.WOOD)));
    public static final DeferredItem<BlockItem> SHIP_HELM_ITEM = ITEMS.register("ship_helm",
            () -> new BlockItem(SHIP_HELM.get(), new Item.Properties()));
    public static final DeferredItem<ShipToolItem> SHIP_TOOL = ITEMS.register("ship_tool",
            () -> new ShipToolItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<SwordItem> WARRIOR_BLADE = ITEMS.register("warrior_blade",
            () -> new SwordItem(Tiers.IRON, new Item.Properties()));
    /** Example custom battle consumable. Its battle behaviour is registered by BattleItemRegistry, not by the item class. */
    public static final DeferredItem<Item> BATTLE_ELIXIR = ITEMS.register("battle_elixir",
            () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<StorageCoreItem> BASIC_STORAGE_CORE = ITEMS.register("basic_storage_core",
            () -> new StorageCoreItem(new Item.Properties().stacksTo(1), 27));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DungeonEntranceBlockEntity>> DUNGEON_ENTRANCE_ENTITY = BLOCK_ENTITIES.register("dungeon_entrance",
            () -> BlockEntityType.Builder.of(DungeonEntranceBlockEntity::new, DUNGEON_ENTRANCE.get()).build(null));
    public static final DeferredHolder<EntityType<?>, EntityType<ShipEntity>> SHIP = ENTITIES.register("ship",
            () -> EntityType.Builder.of(ShipEntity::new, MobCategory.MISC).sized(1.0F, 1.0F).clientTrackingRange(10).updateInterval(1).fireImmune().build("exworld:ship"));
    public static final DeferredHolder<EntityType<?>, EntityType<UrbanNpc>> URBAN_NPC = ENTITIES.register("urban_npc",
            () -> EntityType.Builder.of(UrbanNpc::new, MobCategory.CREATURE).sized(0.6F, 1.8F).clientTrackingRange(8).updateInterval(3).build("exworld:urban_npc"));
    public static final DeferredItem<NpcWandItem> NPC_WAND = ITEMS.register("npc_wand",
            () -> new NpcWandItem(new Item.Properties().stacksTo(1)));

    private ExWorldContent() {}

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        ENTITIES.register(bus);
        InventoryRegistries.register(bus);
    }
}
