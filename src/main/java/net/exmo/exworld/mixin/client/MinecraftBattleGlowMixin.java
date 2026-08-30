package net.exmo.exworld.mixin.client;

import net.exmo.exworld.client.battle.BattleClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Makes tactical cursor highlighting authoritative at Minecraft's outline decision seam. */
@Mixin(Minecraft.class)
public abstract class MinecraftBattleGlowMixin {
    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void exworld$battleHoveredGlow(Entity entity, CallbackInfoReturnable<Boolean> callback) {
        if (BattleClient.shouldGlow(entity)) callback.setReturnValue(true);
    }
}
