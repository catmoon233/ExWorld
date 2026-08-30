package net.exmo.exworld.mixin.client;

import com.mojang.blaze3d.audio.ListenerTransform;
import net.exmo.exworld.client.battle.BattleAudioPolicy;
import net.minecraft.client.Camera;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Keeps battle audio centred on the controlled player while retaining camera orientation for stereo direction. */
@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {
    @ModifyArg(method = "updateSource", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/audio/ListenerTransform;<init>(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)V"), index = 0)
    private Vec3 exworld$useBattlePlayerAsListener(Vec3 position) {
        return BattleAudioPolicy.listenerPosition(position);
    }
}
