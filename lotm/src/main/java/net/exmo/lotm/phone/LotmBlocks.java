package net.exmo.lotm.phone;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class LotmBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("lotm");
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, "lotm");
     public static final DeferredBlock<Block> WIFI = BLOCKS.register("wifi_router", () -> new WifiRouterBlock(
             BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(2.5F, 6.0F).lightLevel(state -> 9).sound(SoundType.METAL)));
     public static final DeferredBlock<Block> CHARGER = BLOCKS.register("phone_charger", () -> new PhoneChargerBlock(
             BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(2.0F, 6.0F).lightLevel(state -> 12).sound(SoundType.METAL)));
     public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WifiRouterBlockEntity>> WIFI_ENTITY = BLOCK_ENTITIES.register("wifi_router",
             () -> BlockEntityType.Builder.of(WifiRouterBlockEntity::new, WIFI.get()).build(null));
     public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PhoneChargerBlockEntity>> CHARGER_ENTITY = BLOCK_ENTITIES.register("phone_charger",
             () -> BlockEntityType.Builder.of(PhoneChargerBlockEntity::new, CHARGER.get()).build(null));

    private LotmBlocks() {}

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        BLOCK_ENTITIES.register(bus);
    }
}
