package io.redspace.irons_artifice.client.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public class MuzzleFlashParticleOption implements ParticleOptions {
    public static MapCodec<MuzzleFlashParticleOption> codec(ParticleType<MuzzleFlashParticleOption> type) {
        return RecordCodecBuilder.mapCodec(builder -> builder.group(
                Codec.FLOAT.fieldOf("r").forGetter(MuzzleFlashParticleOption::r),
                Codec.FLOAT.fieldOf("g").forGetter(MuzzleFlashParticleOption::g),
                Codec.FLOAT.fieldOf("b").forGetter(MuzzleFlashParticleOption::b)
        ).apply(builder, (r, g, b) -> new MuzzleFlashParticleOption(type, r, g, b)));
    }

    public static StreamCodec<? super RegistryFriendlyByteBuf, MuzzleFlashParticleOption> streamCodec(
            ParticleType<MuzzleFlashParticleOption> type
    ) {
        return StreamCodec.composite(
                ByteBufCodecs.FLOAT, MuzzleFlashParticleOption::r,
                ByteBufCodecs.FLOAT, MuzzleFlashParticleOption::g,
                ByteBufCodecs.FLOAT, MuzzleFlashParticleOption::b,
                (r, g, b) -> new MuzzleFlashParticleOption(type, r, g, b)
        );
    }

    private final ParticleType<MuzzleFlashParticleOption> type;
    private final float r;
    private final float g;
    private final float b;

    public MuzzleFlashParticleOption(ParticleType<MuzzleFlashParticleOption> type, float r, float g, float b) {
        this.type = type;
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public float r() {
        return r;
    }

    public float g() {
        return g;
    }

    public float b() {
        return b;
    }

    public boolean isTinted() {
        return r != 1f || g != 1f || b != 1f;
    }

    @Override
    public ParticleType<MuzzleFlashParticleOption> getType() {
        return type;
    }
}
