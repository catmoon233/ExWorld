package net.exmo.lotm.spell;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

/** Instantly eat held food and gain extra hunger. 5 mana, 5s cooldown. */
public final class DevourSpell extends InstantSpell {
    public static final int EXTRA_HUNGER = 2;

    public DevourSpell() {
        super("devour", SchoolRegistry.BLOOD_RESOURCE, 5.0, 5, SoundEvents.GENERIC_EAT);
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        if (!(entity instanceof Player player)) return false;
        return edible(player, foodStack(player)) != null;
    }

    @Override
    protected void cast(Level level, int spellLevel, LivingEntity caster) {
        if (!(caster instanceof ServerPlayer player)) return;
        ItemStack stack = foodStack(player);
        InteractionHand hand = stack == player.getOffhandItem() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        eat(player, hand);
    }

    public static boolean eat(ServerPlayer player, InteractionHand hand) {
        if (player == null || hand == null) return false;
        ItemStack stack = player.getItemInHand(hand);
        FoodProperties food = edible(player, stack);
        if (food == null) return false;
        player.eat(player.level(), stack, food);
        player.getFoodData().eat(EXTRA_HUNGER, 0.4F);
        player.displayClientMessage(Component.translatable("spell.lotm.devour.extra"), true);
        return true;
    }

    public static ItemStack foodStack(Player player) {
        if (player == null) return ItemStack.EMPTY;
        ItemStack main = player.getMainHandItem();
        if (main.get(DataComponents.FOOD) != null) return main;
        ItemStack off = player.getOffhandItem();
        if (off.get(DataComponents.FOOD) != null) return off;
        return ItemStack.EMPTY;
    }

    public static FoodProperties edible(Player player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) return null;
        FoodProperties food = stack.get(DataComponents.FOOD);
        if (food == null) return null;
        if (!food.canAlwaysEat() && !player.getFoodData().needsFood()) return null;
        return food;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("spell.lotm.devour.guide"));
    }
}
