package net.exmo.lotm.phone;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class LotmItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("lotm");
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "lotm");
    public static final DeferredItem<PhoneItem> PHONE = ITEMS.register("phone",
            () -> new PhoneItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<SimCardItem> SIM = ITEMS.register("sim_card",
            () -> new SimCardItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<MapWandItem> MAP_WAND = ITEMS.register("map_wand",
            () -> new MapWandItem(new Item.Properties().stacksTo(1)));
     public static final DeferredItem<BlockItem> WIFI = ITEMS.register("wifi_router",
             () -> new BlockItem(LotmBlocks.WIFI.get(), new Item.Properties()));
     public static final DeferredItem<BlockItem> CHARGER = ITEMS.register("phone_charger",
             () -> new BlockItem(LotmBlocks.CHARGER.get(), new Item.Properties()));
     public static final DeferredHolder<CreativeModeTab, CreativeModeTab> PHONE_TAB = TABS.register("phone", () -> CreativeModeTab.builder()
             .title(Component.translatable("itemGroup.lotm.phone"))
             .icon(() -> PHONE.get().getDefaultInstance())
             .displayItems((parameters, output) -> {
                 output.accept(PHONE.get());
                 output.accept(WIFI.get());
                 output.accept(SIM.get());
                 output.accept(CHARGER.get());
             })
             .build());

    private LotmItems() {}

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        TABS.register(bus);
        bus.addListener(LotmItems::creative);
    }

    private static void creative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(PHONE);
            event.accept(SIM);
            event.accept(MAP_WAND);
             event.accept(WIFI);
             event.accept(CHARGER);
        }
    }
}
