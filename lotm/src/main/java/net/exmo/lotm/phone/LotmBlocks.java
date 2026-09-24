package net.exmo.lotm.phone;

import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Phone blocks referenced by the item registrar. Models are optional. */
public final class LotmBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("lotm");
    public static final DeferredBlock<Block> WIFI = BLOCKS.registerSimpleBlock("wifi_router");
    public static final DeferredBlock<Block> CHARGER = BLOCKS.registerSimpleBlock("phone_charger");

    private LotmBlocks() {}

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
