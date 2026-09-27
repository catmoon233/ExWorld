package net.exmo.lotm.story;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public final class StoryArmorItem extends ArmorItem {
    public StoryArmorItem(Type type, Properties properties) {
        super(ArmorMaterials.LEATHER, type, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.lotm.basic_armor.desc").withStyle(ChatFormatting.GRAY));
    }
}
