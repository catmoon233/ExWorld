package com.wan.gmmod.common.item;

import com.wan.gmmod.common.registry.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 货币物品：便士 / 苏勒 / 金镑。以「便士」为最小单位承载价值属性
 * （{@code currency_value} 数据组件：1 便士 = 1，1 苏勒 = 12，1 金镑 = 240），
 * 便于交易与物价换算。可通过合成台按 12 便士 = 1 苏勒、20 苏勒 = 1 金镑兑换。
 */
public class CurrencyItem extends Item {

    public CurrencyItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int value = stack.getOrDefault(ModDataComponents.CURRENCY_VALUE.get(), 0);
        if (value > 0) {
            tooltip.add(Component.translatable("item.guimi_mod.currency.value", value));
        }
        tooltip.add(Component.translatable("item.guimi_mod.currency.exchange"));
    }
}