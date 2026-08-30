package net.exmo.exworld.mixin.client;

import net.exmo.exworld.client.perspective.DungeonPerspective;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Routes live mouse input to the detached dungeon camera without fighting the player's rotation every tick. */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Redirect(method = "turnPlayer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void exworld$turnDungeonCamera(LocalPlayer player, double yawDelta, double pitchDelta) {
        if (DungeonPerspective.active()) DungeonPerspective.updateMiddleMouseCamera(net.minecraft.client.Minecraft.getInstance());
        else player.turn(yawDelta, pitchDelta);
    }

    @Redirect(method = "grabMouse", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/InputConstants;grabOrReleaseMouse(JIDD)V"))
    private void exworld$showFreeDungeonCursor(long window, int mode, double x, double y) {
        InputConstants.grabOrReleaseMouse(window,
                DungeonPerspective.active() && net.minecraft.client.Minecraft.getInstance().screen == null ? GLFW.GLFW_CURSOR_NORMAL : mode, x, y);
    }
}
