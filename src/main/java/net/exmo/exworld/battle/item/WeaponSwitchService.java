package net.exmo.exworld.battle.item;

import net.exmo.exworld.battle.combatant.Combatant;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Owns the authoritative whole-stack exchange used by battle weapon switching. */
public final class WeaponSwitchService {
    public String switchMainHand(ServerPlayer player, Combatant actor, int weaponSlot) {
        if (player == null || actor == null || weaponSlot < 1 || weaponSlot > 2) return "battle.command.weapon_unavailable";
        ItemStack rail = net.exmo.exworld.inventory.PlayerBackpack.of(player).weapon(weaponSlot - 1);
        if (rail.isEmpty()) return "battle.command.weapon_missing";
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, rail.copy());
        player.getInventory().setChanged();
        actor.setActiveWeaponSlot(weaponSlot);
        return "";
    }

    private static int findItem(Inventory inventory, String itemId) {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && itemId.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())) return i;
        }
        return -1;
    }
}
