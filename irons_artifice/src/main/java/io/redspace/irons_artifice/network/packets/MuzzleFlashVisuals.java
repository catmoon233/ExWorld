package io.redspace.irons_artifice.network.packets;

import io.redspace.irons_artifice.data.ParticleBurst;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record MuzzleFlashVisuals(
        Optional<ParticleOptions> flash,
        List<ParticleBurst> airBursts,
        List<ParticleBurst> underwaterBursts
) {
    public static final StreamCodec<RegistryFriendlyByteBuf, MuzzleFlashVisuals> STREAM_CODEC =
            StreamCodec.of(MuzzleFlashVisuals::encode, MuzzleFlashVisuals::decode);

    private static void encode(RegistryFriendlyByteBuf buf, MuzzleFlashVisuals visuals) {
        buf.writeBoolean(visuals.flash.isPresent());
        visuals.flash.ifPresent(particle -> ParticleTypes.STREAM_CODEC.encode(buf, particle));
        writeBursts(buf, visuals.airBursts);
        writeBursts(buf, visuals.underwaterBursts);
    }

    private static MuzzleFlashVisuals decode(RegistryFriendlyByteBuf buf) {
        Optional<ParticleOptions> flash = buf.readBoolean()
                ? Optional.of(ParticleTypes.STREAM_CODEC.decode(buf))
                : Optional.empty();
        return new MuzzleFlashVisuals(flash, readBursts(buf), readBursts(buf));
    }

    private static void writeBursts(RegistryFriendlyByteBuf buf, List<ParticleBurst> bursts) {
        buf.writeVarInt(bursts.size());
        for (ParticleBurst burst : bursts) {
            ParticleBurst.STREAM_CODEC.encode(buf, burst);
        }
    }

    private static List<ParticleBurst> readBursts(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<ParticleBurst> bursts = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            bursts.add(ParticleBurst.STREAM_CODEC.decode(buf));
        }
        return bursts;
    }
}
