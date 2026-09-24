package net.exmo.exworld.mixin;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Prefixes a spectator's visible name with [旁观].
 *
 * <p>1.21.1 {@code LivingEntity} has no {@code getTeamName}. Chat, nametags and the tab list all consume
 * {@link Player#getDisplayName()}, which is the team-formatted name.</p>
 */
@Mixin(Player.class)
public abstract class PlayerSpectatorNameMixin {
    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void exworld$spectatorTag(CallbackInfoReturnable<Component> callback) {
        Player self = (Player) (Object) this;
        if (!self.isSpectator()) return;
        Component name = callback.getReturnValue();
        if (name == null || name.getString().startsWith("[旁观]")) return;
        callback.setReturnValue(Component.literal("[旁观] ").withStyle(ChatFormatting.GRAY).append(name));
    }
}
