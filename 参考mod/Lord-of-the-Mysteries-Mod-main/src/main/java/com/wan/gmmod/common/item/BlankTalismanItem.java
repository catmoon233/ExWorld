package com.wan.gmmod.common.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 空白符咒：门途径（秘法师 / 记录官等）书写符咒的基础载体。
 * <p>
 * 暂无消耗型功能，作为素材 / 收藏品，等待被注入灵性书写符文。
 */
public class BlankTalismanItem extends Item {
    public BlankTalismanItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.guimi_mod.blank_talisman.tooltip"));
    }
}
