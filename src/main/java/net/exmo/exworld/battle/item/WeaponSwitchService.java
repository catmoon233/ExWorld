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
        String wanted = actor.weaponItem(weaponSlot);
        if (wanted.isBlank()) return "battle.command.weapon_unavailable";
        Inventory inventory = player.getInventory();
        int target = findItem(inventory, wanted);
        if (target < 0) return "battle.command.weapon_missing";
        int selected = inventory.selected;
        if (target != selected) {
            ItemStack held = inventory.getItem(selected).copy();
            inventory.setItem(selected, inventory.getItem(target).copy());
            inventory.setItem(target, held);
        }
        inventory.setChanged();
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
