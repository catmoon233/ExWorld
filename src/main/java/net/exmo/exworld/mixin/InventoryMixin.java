package net.exmo.exworld.mixin;

import net.exmo.exworld.inventory.BackpackPickup;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Inventory.class)
public abstract class InventoryMixin {
    @Shadow @Final public Player player;

    @Inject(method = "add(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void exworld$admit(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        int offer = BackpackPickup.offer(player, stack);
        if (offer != BackpackPickup.PASS) cir.setReturnValue(offer == BackpackPickup.ACCEPT);
    }

}
