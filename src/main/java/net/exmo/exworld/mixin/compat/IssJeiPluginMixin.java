package net.exmo.exworld.mixin.compat;

import mezz.jei.api.registration.IAdvancedRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * Iron's Spellbooks 3.16.x still calls JEI types removed in 19.8
 * ({@code ISubtypeInterpreter}, {@code ISimpleRecipeManagerPlugin}).
 * Those two entry points are skipped so the plugin class can load and the remaining
 * recipe categories still register.
 */
@Mixin(targets = "io.redspace.ironsspellbooks.jei.JeiPlugin", remap = false)
public class IssJeiPluginMixin {
    @Overwrite(remap = false)
    public void registerItemSubtypes(ISubtypeRegistration registration) {
    }

    @Overwrite(remap = false)
    public void registerAdvanced(IAdvancedRegistration registration) {
    }
}
