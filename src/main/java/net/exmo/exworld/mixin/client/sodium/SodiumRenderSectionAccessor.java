package net.exmo.exworld.mixin.client.sodium;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Compile-time-independent access to Sodium's RenderSection origin. */
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.RenderSection", remap = false)
public interface SodiumRenderSectionAccessor {
    @Invoker("getOriginX") int exworld$getOriginX();
    @Invoker("getOriginZ") int exworld$getOriginZ();
}
