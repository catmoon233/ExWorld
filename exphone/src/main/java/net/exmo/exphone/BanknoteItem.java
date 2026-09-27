package net.exmo.exphone;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public final class BanknoteItem extends Item {
    private final int value;

    public BanknoteItem(int value, Properties properties) {
        super(properties);
        this.value = value;
    }

    public int value() {
        return value;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.exphone.banknote.value", value));
    }
}
