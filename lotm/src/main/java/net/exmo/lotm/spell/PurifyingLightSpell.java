package net.exmo.lotm.spell;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import net.exmo.lotm.LotmSupport;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** A short holy pillar. Undead take the full burn; other creatures take half. */
public final class PurifyingLightSpell extends AimedSpell {
    public static final double RANGE = 20.0;
    private static final float DAMAGE = 6.0F;

    public PurifyingLightSpell() {
        super("purifying_light", 10.0, 20, SchoolRegistry.HOLY_RESOURCE, SoundEvents.BEACON_ACTIVATE,
                "spell.lotm.purifying_light.guide");
    }

    @Override
    protected void cast(ServerLevel level, int spellLevel, LivingEntity caster) {
        Vec3 point = LotmSupport.lookedAtPoint(caster, RANGE);
        double x = point.x;
        double y = point.y;
        double z = point.z;
        level.playSound(null, x, y, z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.7F, 1.4F);
        for (int i = 0; i < 8; i++) {
            level.sendParticles(ParticleTypes.END_ROD, x, y + i * 0.45, z, 3, 0.12, 0.2, 0.12, 0.01);
            level.sendParticles(ParticleTypes.FLAME, x, y + i * 0.35, z, 2, 0.15, 0.15, 0.15, 0.01);
        }
        AABB column = new AABB(x - 1.5, y - 0.5, z - 1.5, x + 1.5, y + 4.5, z + 1.5);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, column, LivingEntity::isAlive)) {
            if (LotmSupport.allied(caster, target)) continue;
            boolean undead = LotmSupport.undead(target);
            target.hurt(getDamageSource(caster), undead ? DAMAGE : DAMAGE * 0.5F);
            target.setRemainingFireTicks(undead ? 60 : 30);
        }
    }
}
