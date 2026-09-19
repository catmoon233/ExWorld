package net.exmo.exworld.inventory;

import net.exmo.exworld.Exworld;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import com.mojang.serialization.Codec;

public final class InventoryRegistries {
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Exworld.MODID);
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Exworld.MODID);
    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Exworld.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> ROTATED =
            COMPONENTS.register("rotated", () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerBackpackData>> BACKPACK =
            ATTACHMENTS.register("backpack", () -> AttachmentType.serializable(PlayerBackpackData::new).build());

    public static final DeferredHolder<MenuType<?>, MenuType<PlayerBackpackMenu>> PLAYER_BACKPACK =
            MENUS.register("player_backpack", () -> IMenuTypeExtension.create(PlayerBackpackMenu::new));

    private InventoryRegistries() {}

    public static void register(IEventBus bus) {
        COMPONENTS.register(bus);
        ATTACHMENTS.register(bus);
        MENUS.register(bus);
    }

    public static boolean rotated(ItemStack stack) {
        return stack != null && !stack.isEmpty() && Boolean.TRUE.equals(stack.get(ROTATED.get()));
    }

    public static ItemStack withRotated(ItemStack stack, boolean rotated) {
        if (stack == null || stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack copy = stack.copy();
        if (rotated) copy.set(ROTATED.get(), true);
        else copy.remove(ROTATED.get());
        return copy;
    }
}
