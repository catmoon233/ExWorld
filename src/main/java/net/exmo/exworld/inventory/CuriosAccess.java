package net.exmo.exworld.inventory;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Loaded only after {@link CuriosPresence#loaded()} is true. */
final class CuriosAccess {
    private static final ResourceLocation FALLBACK =
            ResourceLocation.fromNamespaceAndPath("curios", "slot/empty_curio_slot");

    private CuriosAccess() {}

    static List<CuriosPresence.GroupView> groups(Player player) {
        if (player == null) return List.of();
        List<CuriosPresence.GroupView> groups = new ArrayList<>();
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            List<String> ids = new ArrayList<>(handler.getCurios().keySet());
            ids.sort(Comparator.comparingInt((String id) -> order(player, id)).thenComparing(id -> id));
            for (String id : ids) {
                ICurioStacksHandler stacks = handler.getStacksHandler(id).orElse(null);
                if (stacks == null || !stacks.isVisible() || stacks.getSlots() <= 0) continue;
                ResourceLocation icon = CuriosApi.getSlot(id, player.level()).map(slot -> slot.getIcon()).orElse(FALLBACK);
                List<CuriosPresence.SlotView> views = new ArrayList<>();
                for (int i = 0; i < stacks.getSlots(); i++) {
                    ItemStack stack = stacks.getStacks().getStackInSlot(i);
                    views.add(new CuriosPresence.SlotView(id, i, stack == null ? ItemStack.EMPTY : stack.copy(), icon));
                }
                groups.add(new CuriosPresence.GroupView(id, order(player, id), views));
            }
        });
        return groups;
    }

    static void click(ServerPlayer player, String identifier, int index) {
        if (player == null || identifier == null || identifier.isBlank()) return;
        if (!(player.containerMenu instanceof PlayerBackpackMenu menu)) return;
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            ICurioStacksHandler stacks = handler.getStacksHandler(identifier).orElse(null);
            if (stacks == null || !stacks.isVisible() || index < 0 || index >= stacks.getSlots()) return;
            ItemStack current = stacks.getStacks().getStackInSlot(index);
            if (current == null) current = ItemStack.EMPTY;
            ItemStack carried = menu.getCarried();
            if (carried.isEmpty()) {
                if (current.isEmpty()) return;
                menu.setCarried(current.copy());
                handler.setEquippedCurio(identifier, index, ItemStack.EMPTY);
                menu.broadcastChanges();
                return;
            }
            if (!CuriosApi.isStackValid(new SlotContext(identifier, player, index, false, true), carried)) return;
            if (!current.isEmpty() && carried.getCount() != 1) return;
            ItemStack place = carried.copy();
            place.setCount(1);
            ItemStack leftover = carried.copy();
            leftover.shrink(1);
            menu.setCarried(current.isEmpty() ? (leftover.isEmpty() ? ItemStack.EMPTY : leftover) : current.copy());
            handler.setEquippedCurio(identifier, index, place);
            menu.broadcastChanges();
        });
    }

    private static int order(Player player, String id) {
        return CuriosApi.getSlot(id, player.level()).map(slot -> slot.getOrder()).orElse(1000);
    }
}
