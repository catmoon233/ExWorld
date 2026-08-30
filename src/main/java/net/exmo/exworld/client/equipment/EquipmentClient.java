package net.exmo.exworld.client.equipment;

import net.exmo.exworld.network.EquipmentActionPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Method;

/** Client-only entry point for the Ctrl+middle-click equipment picker. */
public final class EquipmentClient {
    private static String slot1 = "";
    private static String slot2 = "";

    private EquipmentClient() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(EquipmentClient::middleClickInventory);
    }

    public static void install(net.exmo.exworld.network.EquipmentSnapshotPayload payload) {
        slot1 = payload.slot1(); slot2 = payload.slot2();
        if (Minecraft.getInstance().screen instanceof WeaponSlotPickerScreen screen) screen.rebuildWidgets();
    }

    public static String slot(int slot) { return slot == 1 ? slot1 : slot == 2 ? slot2 : ""; }

    private static void middleClickInventory(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof InventoryScreen screen) || event.getButton() != GLFW.GLFW_MOUSE_BUTTON_MIDDLE
                || !net.minecraft.client.gui.screens.Screen.hasControlDown()) return;
        Slot slot = slotUnderMouse(screen);
        if (slot == null || !slot.hasItem()) return;
        event.setCanceled(true);
        // Slot.index is the menu slot id. The server needs the backing Inventory index;
        // using the menu id makes armor slots resolve as hotbar/main-inventory slots.
        Minecraft.getInstance().setScreen(new WeaponSlotPickerScreen(slot.getContainerSlot()));
    }

    private static Slot slotUnderMouse(AbstractContainerScreen<?> screen) {
        try {
            Method method = AbstractContainerScreen.class.getDeclaredMethod("getSlotUnderMouse");
            method.setAccessible(true);
            return (Slot) method.invoke(screen);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    public static void send(EquipmentActionPayload.Action action, int weaponSlot, int inventorySlot) {
        PacketDistributor.sendToServer(new EquipmentActionPayload(action, weaponSlot, inventorySlot));
    }
}
