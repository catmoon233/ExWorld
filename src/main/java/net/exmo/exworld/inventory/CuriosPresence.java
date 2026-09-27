package net.exmo.exworld.inventory;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

import java.util.List;

/** Curios is optional. Callers only see these types; Curios classes load after {@link #loaded()} is true. */
public final class CuriosPresence {
    public record SlotView(String identifier, int index, ItemStack stack, ResourceLocation icon) {}

    public record GroupView(String identifier, int order, List<SlotView> slots) {}

    private CuriosPresence() {}

    public static boolean loaded() {
        return ModList.get().isLoaded("curios");
    }

    public static List<GroupView> groups(Player player) {
        if (!loaded() || player == null) return List.of();
        return CuriosAccess.groups(player);
    }

    public static void click(ServerPlayer player, String identifier, int index) {
        if (!loaded()) return;
        CuriosAccess.click(player, identifier, index);
    }
}
