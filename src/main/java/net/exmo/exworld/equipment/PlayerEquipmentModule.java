package net.exmo.exworld.equipment;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/** Small persistence seam for the two player-selected weapon slots. */
public final class PlayerEquipmentModule {
    public static final String DATA_NAME = "exworld_player_equipment";

    public PlayerEquipmentSavedData.Slots slots(MinecraftServer server, UUID playerId) {
        return saved(server).get(playerId);
    }

    public boolean set(ServerPlayer player, int slot, int inventorySlot) {
        // Weapon slots are backed by the player's main inventory only. Armor and offhand
        // indices must never be interpreted as a weapon stack to put into the main hand.
        if (slot < 1 || slot > 2 || inventorySlot < 0 || inventorySlot >= 36) return false;
        ItemStack stack = player.getInventory().getItem(inventorySlot);
        if (stack.isEmpty()) return false;
        String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        PlayerEquipmentSavedData.Slots current = slots(player.getServer(), player.getUUID());
        int other = slot == 1 ? 2 : 1;
        if (itemId.equals(current.get(other))) return false;
        saved(player.getServer()).set(player.getUUID(), slot, itemId);
        return true;
    }

    public boolean clear(ServerPlayer player, int slot) {
        if (slot < 1 || slot > 2) return false;
        saved(player.getServer()).set(player.getUUID(), slot, "");
        return true;
    }

    private PlayerEquipmentSavedData saved(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(PlayerEquipmentSavedData.FACTORY, DATA_NAME);
    }
}
