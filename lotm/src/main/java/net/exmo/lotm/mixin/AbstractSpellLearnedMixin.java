package net.exmo.lotm.mixin;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.exmo.lotm.sequence.SequenceSpellOwnership;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sequence spells count as owned without being written into the learned-spell set or a spellbook.
 */
@Mixin(value = AbstractSpell.class, remap = false)
public abstract class AbstractSpellLearnedMixin {
    @Inject(method = "isLearned", at = @At("HEAD"), cancellable = true, remap = false)
    private void exworld$sequenceOwns(Player player, CallbackInfoReturnable<Boolean> cir) {
        AbstractSpell self = (AbstractSpell) (Object) this;
        if (self.getSpellResource() != null && SequenceSpellOwnership.owns(player, self.getSpellResource())) {
            cir.setReturnValue(true);
        }
    }
}
