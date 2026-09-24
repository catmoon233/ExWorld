package net.exmo.lotm.spell;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.function.Consumer;

/** Instant melee cone. Not written into a spellbook; sequence ownership puts it on the wheel. */
public abstract class ConeStrikeSpell extends AbstractSpell {
    private final ResourceLocation spellId;
    private final DefaultConfig defaultConfig;

    protected ConeStrikeSpell(String path, double cooldownSeconds, int mana, int power) {
        this(path, cooldownSeconds, mana, power, SchoolRegistry.EVOCATION_RESOURCE);
    }

    protected ConeStrikeSpell(String path, double cooldownSeconds, int mana, int power, ResourceLocation school) {
        this.spellId = ResourceLocation.fromNamespaceAndPath("lotm", path);
        this.defaultConfig = new DefaultConfig()
                .setMinRarity(SpellRarity.COMMON)
                .setSchoolResource(school)
                .setMaxLevel(5)
                .setCooldownSeconds(cooldownSeconds)
                .setAllowCrafting(false)
                .build();
        this.baseManaCost = mana;
        this.manaCostPerLevel = 0;
        this.baseSpellPower = power;
        this.spellPowerPerLevel = 1;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellId;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public CastType getCastType() {
        return CastType.INSTANT;
    }

    @Override
    public int getCastTime(int spellLevel) {
        return 0;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.PLAYER_ATTACK_SWEEP);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.empty();
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return AnimationHolder.none();
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.none();
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
        if (caster instanceof Player player) player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        Vec3 look = caster.getLookAngle();
        float damage = damageFor(spellLevel, caster);
        double range = range();
        var box = caster.getBoundingBox().expandTowards(look.scale(range)).inflate(1.2);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, living -> living != caster && living.isAlive() && !living.isAlliedTo(caster))) {
            Vec3 to = target.getEyePosition().subtract(caster.getEyePosition());
            double distance = to.length();
            if (distance > range || distance < 1.0E-4) continue;
            if (to.normalize().dot(look) < coneDot()) continue;
            target.hurt(damageSource(caster), damage);
            target.knockback(knockback(), -look.x, -look.z);
            onHit(caster, target, spellLevel);
        }
        super.onCast(level, spellLevel, caster, source, data);
    }

    protected abstract float damageFor(int spellLevel, LivingEntity caster);

    protected double range() {
        return 3.2;
    }

    protected double coneDot() {
        return 0.35;
    }

    protected double knockback() {
        return 0.35;
    }

    protected net.minecraft.world.damagesource.DamageSource damageSource(LivingEntity caster) {
        return getDamageSource(caster);
    }

    protected void onHit(LivingEntity caster, LivingEntity target, int spellLevel) {}

    protected static Consumer<LivingEntity> none() {
        return target -> {};
    }
}
