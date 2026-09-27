package com.wan.gmmod.common.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class SpiritRodItem extends FishingRodItem {
    public SpiritRodItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.guimi_mod.spirit_rod.tooltip"));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}