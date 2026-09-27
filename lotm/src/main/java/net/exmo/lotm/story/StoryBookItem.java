package net.exmo.lotm.story;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public final class StoryBookItem extends Item {
    private final String pagesKey;

    public StoryBookItem(Properties properties, String pagesKey) {
        super(properties);
        this.pagesKey = pagesKey;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) StoryClientHooks.openBook.accept(pagesKey);
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String id = BuiltInRegistries.ITEM.getKey(this).getPath();
        tooltip.add(Component.translatable("item.lotm." + id + ".desc").withStyle(ChatFormatting.GRAY));
    }
}
