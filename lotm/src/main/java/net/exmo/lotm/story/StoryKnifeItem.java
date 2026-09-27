package net.exmo.lotm.story;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public final class StoryKnifeItem extends SwordItem {
    private final String desc;

    public StoryKnifeItem(Properties properties, String desc) {
        super(Tiers.IRON, properties);
        this.desc = desc;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(desc).withStyle(ChatFormatting.GRAY));
    }
}
