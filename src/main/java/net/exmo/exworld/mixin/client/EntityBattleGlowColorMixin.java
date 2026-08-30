package net.exmo.exworld.mixin.client;

import net.exmo.exworld.client.battle.BattleClient;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Applies the temporary tactical cast color without changing the entity's real scoreboard team. */
@Mixin(Entity.class)
public abstract class EntityBattleGlowColorMixin {
    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    private void exworld$battleCastGlow(CallbackInfoReturnable<Boolean> callback) {
        if (BattleClient.shouldCastGlow((Entity) (Object) this)) callback.setReturnValue(true);
    }

    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void exworld$battleCastGlowColor(CallbackInfoReturnable<Integer> callback) {
        int color = BattleClient.castGlowColor((Entity) (Object) this);
        if (color != 0) callback.setReturnValue(color);
    }
}
