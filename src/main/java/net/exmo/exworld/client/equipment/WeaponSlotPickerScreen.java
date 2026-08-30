package net.exmo.exworld.client.equipment;

import net.exmo.exworld.network.EquipmentActionPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Small, deliberately server-driven picker opened from a vanilla inventory slot. */
public final class WeaponSlotPickerScreen extends Screen {
    private final int inventorySlot;

    public WeaponSlotPickerScreen(int inventorySlot) {
        super(Component.translatable("screen.exworld.weapon_slots"));
        this.inventorySlot = inventorySlot;
    }

    @Override protected void init() { rebuildWidgets(); }

    @Override
    public void renderBackground(GuiGraphics p_283688_, int p_296369_, int p_296477_, float p_294317_) {
        
    }

    public void rebuildWidgets() {
        if (minecraft == null) return;
        clearWidgets();
        int x = width / 2 - 90, y = height / 2 - 42;
        addRenderableWidget(Button.builder(Component.translatable("screen.exworld.weapon_slot_1"), button -> choose(1)).bounds(x, y, 180, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.exworld.weapon_slot_2"), button -> choose(2)).bounds(x, y + 24, 180, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.exworld.weapon_clear_1"), button -> clear(1)).bounds(x, y + 48, 88, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.exworld.weapon_clear_2"), button -> clear(2)).bounds(x + 92, y + 48, 88, 20).build());
    }

    private void choose(int weaponSlot) {
        EquipmentClient.send(EquipmentActionPayload.Action.SET, weaponSlot, inventorySlot);
        onClose();
    }

    private void clear(int weaponSlot) {
        EquipmentClient.send(EquipmentActionPayload.Action.CLEAR, weaponSlot, -1);
        onClose();
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 70, 0xFFFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("screen.exworld.weapon_current", EquipmentClient.slot(1), EquipmentClient.slot(2)), width / 2, height / 2 - 58, 0xFFAAAAAA);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
