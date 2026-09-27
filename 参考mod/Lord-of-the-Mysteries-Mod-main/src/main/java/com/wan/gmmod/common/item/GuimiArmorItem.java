package com.wan.gmmod.common.item;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;

public class GuimiArmorItem extends ArmorItem {

    public GuimiArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, net.minecraft.world.level.Level level, Entity entity, int slot, boolean selected) {
        if (slot == EquipmentSlot.HEAD.getIndex() && entity instanceof net.minecraft.world.entity.player.Player player) {
        }
    }
}