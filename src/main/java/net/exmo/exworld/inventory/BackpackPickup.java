package net.exmo.exworld.inventory;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Pickup hook used by the inventory mixin. Kept separate so mixin attachment
 * does not have to resolve {@link PlayerBackpack} while {@code MenuType} is still initializing.
 */
public final class BackpackPickup {
    public static final int PASS = 0;
    public static final int ACCEPT = 1;
    public static final int REJECT = 2;

    private BackpackPickup() {}

    public static int offer(Player player, ItemStack stack) {
        if (PlayerBackpack.mutating() || !(player instanceof ServerPlayer serverPlayer) || serverPlayer.isCreative()) {
            return PASS;
        }
        return PlayerBackpack.of(serverPlayer).admit(stack) ? ACCEPT : REJECT;
    }
}
