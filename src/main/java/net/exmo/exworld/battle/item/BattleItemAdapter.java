package net.exmo.exworld.battle.item;

import net.minecraft.world.item.ItemStack;

/** Code-registered projection and targeting seam for a Minecraft inventory item. */
public interface BattleItemAdapter {
    String id();
    boolean matches(ItemStack stack);
    String targetType();

    /** Returns a translated command key when the target is invalid, or an empty string when valid. */
    default String validate(BattleItemUseContext context) { return ""; }

    /** Applies the effect. Returning a non-empty key aborts the use without consuming the stack. */
    String use(BattleItemUseContext context);
}
