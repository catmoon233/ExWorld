package net.exmo.exworld.mixin.client;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Accesses vanilla's final FOV calculation so projection matches every live camera modifier. */
@Mixin(GameRenderer.class)
public interface GameRendererAccessor {
    @Invoker("getFov")
    double exworld$renderedFov(Camera camera, float partialTick, boolean useFovSetting);
}
