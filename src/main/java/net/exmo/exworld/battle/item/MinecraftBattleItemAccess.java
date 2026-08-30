package net.exmo.exworld.battle.item;

import net.exmo.exworld.battle.BattleSession;
import net.exmo.exworld.battle.api.BattleCommand;
import net.exmo.exworld.battle.api.BattleSnapshot;
import net.exmo.exworld.battle.combatant.Combatant;
import net.exmo.exworld.battle.model.BattleCell;
import net.exmo.exworld.equipment.PlayerEquipmentModule;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Minecraft adapter for real player inventory, potion and weapon state. */
public final class MinecraftBattleItemAccess implements BattleItemAccess {
    private final PlayerEquipmentModule equipment;
    private final BattleItemRegistry registry;
    private final WeaponSwitchService weaponSwitch = new WeaponSwitchService();
    private final Map<UUID, PendingVisualUse> pendingUses = new HashMap<>();

    public MinecraftBattleItemAccess(PlayerEquipmentModule equipment) { this(equipment, BattleItemRegistry.defaults()); }
    public MinecraftBattleItemAccess(PlayerEquipmentModule equipment, BattleItemRegistry registry) { this.equipment = equipment; this.registry = registry; }
    public MinecraftBattleItemAccess register(BattleItemAdapter adapter) { registry.register(adapter); return this; }

    @Override public String useItem(BattleSession session, Combatant actor, BattleCommand.UseItem command) {
        if (actor.playerId() == null || session.state() != net.exmo.exworld.battle.model.BattleState.FACTION_PHASE)
            return "battle.command.item_unavailable";
        ServerPlayer player = net.exmo.exworld.battle.BattleSystem.player(actor.playerId());
        if (player == null || actor.downed() || session.ready(actor) || !actor.factionId().equals(session.snapshot().activeFaction())) return "battle.command.item_unavailable";
        if (pendingUses.containsKey(actor.playerId())) return "battle.command.actor_busy";
        if (actor.itemUsesRemaining() <= 0) return "battle.command.item_limit";
        // Only the 36 main-inventory slots can be projected into the main hand.
        // Armor/offhand indices are not consumable battle inventory slots.
        if (command.inventorySlot() < 0 || command.inventorySlot() >= 36) return "battle.command.item_missing";
        ItemStack stack = player.getInventory().getItem(command.inventorySlot());
        String itemId = itemId(stack);
        if (stack.isEmpty() || !itemId.equals(command.itemId())) return "battle.command.item_missing";
        if (session.actionBusy(actor.id())) return "battle.command.actor_busy";
        BattleItemAdapter adapter = registry.find(stack).orElse(null);
        if (adapter == null) return "battle.command.item_unadapted";
        BattleItemUseContext context = new BattleItemUseContext(session, actor, player, stack, command.targetCell());
        String validation = adapter.validate(context);
        if (!validation.isEmpty()) return validation;

        // Present the real stack in the player's hand so vanilla first-person/third-person
        // use animation and item-use state are visible. The logical battle adapter still owns
        // the authoritative effect; the short visual lease is restored below.
        boolean sourceWasMainHand = player.getInventory().selected == command.inventorySlot();
        ItemStack previousMainHand = player.getMainHandItem().copy();
        if (!sourceWasMainHand) player.setItemInHand(InteractionHand.MAIN_HAND, stack.copy());
        player.startUsingItem(InteractionHand.MAIN_HAND);
        player.swing(InteractionHand.MAIN_HAND, true);

        String failure;
        try {
            failure = adapter.use(context);
        } catch (RuntimeException exception) {
            restoreVisualUse(player, new PendingVisualUse(command.inventorySlot(), sourceWasMainHand, previousMainHand, 0));
            return "battle.command.item_unavailable";
        }
        if (!failure.isEmpty()) {
            restoreVisualUse(player, new PendingVisualUse(command.inventorySlot(), sourceWasMainHand, previousMainHand, 0));
            return failure;
        }
        stack.shrink(1); player.getInventory().setChanged(); actor.consumeItemUse();
        pendingUses.put(actor.playerId(), new PendingVisualUse(command.inventorySlot(), sourceWasMainHand, previousMainHand, 8));
        syncInventory(player);
        return "";
    }

    @Override public String switchWeapon(BattleSession session, Combatant actor, BattleCommand.SwitchWeapon command) {
        if (actor.playerId() == null || command.weaponSlot() < 1 || command.weaponSlot() > 2) return "battle.command.weapon_unavailable";
        ServerPlayer player = net.exmo.exworld.battle.BattleSystem.player(actor.playerId());
        if (player == null) return "battle.command.weapon_unavailable";
        if (pendingUses.containsKey(actor.playerId())) return "battle.command.actor_busy";
        return weaponSwitch.switchMainHand(player, actor, command.weaponSlot());
    }

    /**
     * Equips weapon slot 1 when a player enters or reconnects to a battle.
     *
     * An empty/air configuration is intentionally a no-op.  A configured weapon
     * that is not currently present is also left alone so entering a battle never
     * unexpectedly changes the player's selected item.  The switch itself uses
     * the same main-inventory-only service as the explicit battle command, so the
     * complete stack (including NBT, durability and count) is preserved and armor
     * slots can never be selected by this path.
     */
    public boolean autoEquipWeaponOne(BattleSession session, Combatant actor, ServerPlayer player) {
        if (actor == null || actor.playerId() == null || player == null) return false;
        String wanted = actor.weaponItem(1);
        if (wanted == null || wanted.isBlank() || "minecraft:air".equals(wanted)) {
            refreshActiveWeapon(session, actor, player);
            return false;
        }
        if (!wanted.equals(itemId(player.getMainHandItem()))) {
            if (!weaponSwitch.switchMainHand(player, actor, 1).isEmpty()) {
                refreshActiveWeapon(session, actor, player);
                return false;
            }
        }
        refreshActiveWeapon(session, actor, player);
        return true;
    }

    /** Advances and restores the short real-item use presentation on the server thread. */
    public void tick() {
        var iterator = pendingUses.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            PendingVisualUse use = entry.getValue();
            ServerPlayer player = net.exmo.exworld.battle.BattleSystem.player(entry.getKey());
            if (player == null || use.ticksRemaining() <= 1) {
                if (player != null) restoreVisualUse(player, use);
                iterator.remove();
            } else {
                entry.setValue(use.withTicksRemaining(use.ticksRemaining() - 1));
            }
        }
    }

    private static void restoreVisualUse(ServerPlayer player, PendingVisualUse use) {
        player.stopUsingItem();
        if (!use.sourceWasMainHand()) player.setItemInHand(InteractionHand.MAIN_HAND, use.previousMainHand());
        syncInventory(player);
    }

    private static void syncInventory(ServerPlayer player) {
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        player.inventoryMenu.broadcastChanges();
    }

    @Override public List<BattleSnapshot.ItemView> items(Combatant actor) {
        if (actor.playerId() == null) return List.of();
        ServerPlayer player = net.exmo.exworld.battle.BattleSystem.player(actor.playerId());
        if (player == null) return List.of();
        List<BattleSnapshot.ItemView> result = new ArrayList<>();
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i); if (stack.isEmpty()) continue;
            BattleItemAdapter adapter = registry.find(stack).orElse(null);
            if (i >= 36 || adapter == null || actor.itemUsesRemaining() <= 0) continue;
            result.add(new BattleSnapshot.ItemView(i, itemId(stack), stack.getHoverName().getString(), stack.getCount(), true,
                    adapter.targetType()));
        }
        return List.copyOf(result);
    }

    @Override public List<BattleSnapshot.WeaponSlotView> weaponSlots(BattleSession session, Combatant actor) {
        if (actor.playerId() == null) return List.of();
        ServerPlayer player = net.exmo.exworld.battle.BattleSystem.player(actor.playerId());
        if (player == null) return List.of();
        List<BattleSnapshot.WeaponSlotView> result = new ArrayList<>();
        for (int slot = 1; slot <= 2; slot++) {
            String id = actor.weaponItem(slot); int index = findItem(player.getInventory(), id);
            ItemStack stack = index < 0 ? ItemStack.EMPTY : player.getInventory().getItem(index);
            result.add(new BattleSnapshot.WeaponSlotView(slot, id, stack.isEmpty() ? id : stack.getHoverName().getString(),
                    index >= 0, session.passiveEngine().passiveId(id), session.passiveEngine().passiveProgress(id, actor)));
        }
        return List.copyOf(result);
    }

    private static int findItem(Inventory inventory, String itemId) {
        for (int i = 0; i < 36; i++) if (itemId.equals(itemId(inventory.getItem(i)))) return i;
        return -1;
    }
    private static String itemId(ItemStack stack) { return stack.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(); }

    /** Reconciles passive activation with the real selected hotbar stack after a vanilla hotkey change. */
    public void refreshActiveWeapon(BattleSession session, Combatant actor) {
        if (actor == null || actor.playerId() == null) return;
        ServerPlayer player = net.exmo.exworld.battle.BattleSystem.player(actor.playerId());
        refreshActiveWeapon(session, actor, player);
    }

    private void refreshActiveWeapon(BattleSession session, Combatant actor, ServerPlayer player) {
        if (actor == null || player == null) return;
        String held = itemId(player.getMainHandItem());
        int slot = held.equals(actor.weaponItem(1)) && !held.isBlank() ? 1 : held.equals(actor.weaponItem(2)) && !held.isBlank() ? 2 : 0;
        session.syncActiveWeapon(actor, slot);
    }

    private record PendingVisualUse(int sourceSlot, boolean sourceWasMainHand, ItemStack previousMainHand,
                                    int ticksRemaining) {
        private PendingVisualUse withTicksRemaining(int ticks) {
            return new PendingVisualUse(sourceSlot, sourceWasMainHand, previousMainHand, ticks);
        }
    }
}
