package net.exmo.exworld.mixin.client;

import io.redspace.ironsspellbooks.gui.overlays.SpellBarOverlay;
import net.exmo.exworld.client.battle.BattleClient;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hides Iron's Spells 'n Spellbooks spell bar while a battle session is active.
 * The battle HUD replaces the whole vanilla/actionbar layout, so the external
 * spell bar (anchored near the hotbar) would otherwise float over the card hand.
 */
@Mixin(SpellBarOverlay.class)
public abstract class SpellBarOverlayMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void exworld$hideSpellBarDuringBattle(GuiGraphics guiHelper, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (BattleClient.active()) {
            ci.cancel();
        }
    }
}
