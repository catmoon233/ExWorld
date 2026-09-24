package io.redspace.irons_artifice.client.particle;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public class MuzzleFlashParticleType extends ParticleType<MuzzleFlashParticleOption> {
    public MuzzleFlashParticleType(boolean overrideLimiter) {
        super(overrideLimiter);
    }

    @Override
    public MapCodec<MuzzleFlashParticleOption> codec() {
        return MuzzleFlashParticleOption.codec(this);
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, MuzzleFlashParticleOption> streamCodec() {
        return MuzzleFlashParticleOption.streamCodec(this);
    }
}
