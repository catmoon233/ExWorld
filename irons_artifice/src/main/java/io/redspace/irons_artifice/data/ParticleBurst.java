package io.redspace.irons_artifice.data;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record ParticleBurst(
        ParticleOptions particle,
        int count,
        float velocityScale,
        boolean force
) {
    public static final ParticleBurst SMOKE =
            new ParticleBurst(ParticleTypes.SMOKE, 12, 0.25f, true);
    public static final ParticleBurst BUBBLES =
            new ParticleBurst(ParticleTypes.BUBBLE, 40, 1.75f, false);

    public static final StreamCodec<RegistryFriendlyByteBuf, ParticleBurst> STREAM_CODEC =
            StreamCodec.of(ParticleBurst::encode, ParticleBurst::decode);

    private static void encode(RegistryFriendlyByteBuf buf, ParticleBurst burst) {
        ParticleTypes.STREAM_CODEC.encode(buf, burst.particle);
        buf.writeVarInt(burst.count);
        buf.writeFloat(burst.velocityScale);
        buf.writeBoolean(burst.force);
    }

    private static ParticleBurst decode(RegistryFriendlyByteBuf buf) {
        return new ParticleBurst(
                ParticleTypes.STREAM_CODEC.decode(buf),
                buf.readVarInt(),
                buf.readFloat(),
                buf.readBoolean()
        );
    }
}
