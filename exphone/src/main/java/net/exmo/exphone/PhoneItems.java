package net.exmo.exphone;

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

public final class PhoneItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ExPhone.MODID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ExPhone.MODID);
    public static final DeferredItem<PhoneItem> PHONE = ITEMS.register("phone",
            () -> new PhoneItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<SimCardItem> SIM = ITEMS.register("sim_card",
            () -> new SimCardItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<MapWandItem> MAP_WAND = ITEMS.register("map_wand",
            () -> new MapWandItem(new Item.Properties().stacksTo(1)));
     public static final DeferredItem<BlockItem> WIFI = ITEMS.register("wifi_router",
             () -> new BlockItem(PhoneBlocks.WIFI.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> CHARGER = ITEMS.register("phone_charger",
            () -> new BlockItem(PhoneBlocks.CHARGER.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> ATM = ITEMS.register("atm",
            () -> new BlockItem(PhoneBlocks.ATM.get(), new Item.Properties()));
    public static final DeferredItem<BanknoteItem> BILL_1 = bill("banknote_1", 1);
    public static final DeferredItem<BanknoteItem> BILL_5 = bill("banknote_5", 5);
    public static final DeferredItem<BanknoteItem> BILL_10 = bill("banknote_10", 10);
    public static final DeferredItem<BanknoteItem> BILL_20 = bill("banknote_20", 20);
    public static final DeferredItem<BanknoteItem> BILL_50 = bill("banknote_50", 50);
    public static final DeferredItem<BanknoteItem> BILL_100 = bill("banknote_100", 100);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> PHONE_TAB = TABS.register("phone", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.exphone.phone"))
            .icon(() -> PHONE.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(PHONE.get());
                output.accept(WIFI.get());
                output.accept(SIM.get());
                output.accept(CHARGER.get());
                output.accept(ATM.get());
                output.accept(MAP_WAND.get());
                for (int value : Banknotes.VALUES) output.accept(billItem(value));
            })
            .build());

    private PhoneItems() {}

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        TABS.register(bus);
        bus.addListener(PhoneItems::creative);
    }

    public static Item bill(int value) {
        DeferredItem<BanknoteItem> item = billItem(value);
        return item == null ? net.minecraft.world.item.Items.AIR : item.get();
    }

    private static DeferredItem<BanknoteItem> billItem(int value) {
        return switch (value) {
            case 1 -> BILL_1;
            case 5 -> BILL_5;
            case 10 -> BILL_10;
            case 20 -> BILL_20;
            case 50 -> BILL_50;
            case 100 -> BILL_100;
            default -> null;
        };
    }

    private static DeferredItem<BanknoteItem> bill(String name, int value) {
        return ITEMS.register(name, () -> new BanknoteItem(value, new Item.Properties().stacksTo(64)));
    }

    private static void creative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(PHONE);
            event.accept(SIM);
            event.accept(MAP_WAND);
            event.accept(WIFI);
            event.accept(CHARGER);
            event.accept(ATM);
            for (int value : Banknotes.VALUES) event.accept(billItem(value));
        }
    }
}
