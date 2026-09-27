package net.exmo.lotm.story;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Plays a chime loop in place of 《雨爱》 until an ogg is added. */
public final class SpeakerItem extends Item {
    public SpeakerItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player) || player.tickCount % 80 != 0) return;
        if (!firstSpeaker(player, slot)) return;
        level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.RECORDS, 0.55F, pitch(player.tickCount));
    }

    private static boolean firstSpeaker(Player player, int slot) {
        for (int i = 0; i < slot; i++) {
            if (player.getInventory().getItem(i).is(StoryItems.SPEAKER.get())) return false;
        }
        return true;
    }

    private static float pitch(int tick) {
        float[] notes = {0.9F, 1.0F, 1.2F, 1.0F, 0.8F, 1.0F};
        return notes[(tick / 80) % notes.length];
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.lotm.speaker.desc").withStyle(ChatFormatting.GRAY));
    }
}
