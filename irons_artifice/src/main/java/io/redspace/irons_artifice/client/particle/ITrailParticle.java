package io.redspace.irons_artifice.client.particle;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;

public interface ITrailParticle {
    ParticleOptions applyTrailInterpolation(ParticleOptions base, float percent);
}
