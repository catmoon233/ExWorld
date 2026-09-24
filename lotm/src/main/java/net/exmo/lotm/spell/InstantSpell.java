package net.exmo.lotm.spell;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.Optional;

/** Instant sequence active. Not crafted into a spellbook; ownership puts it on the wheel. */
public abstract class InstantSpell extends AbstractSpell {
    private final ResourceLocation spellId;
    private final DefaultConfig defaultConfig;
    private final SoundEvent castSound;

    protected InstantSpell(String path, ResourceLocation school, double cooldownSeconds, int mana, SoundEvent castSound) {
        this.spellId = ResourceLocation.fromNamespaceAndPath("lotm", path);
        this.castSound = castSound;
        this.defaultConfig = new DefaultConfig()
                .setMinRarity(SpellRarity.COMMON)
                .setSchoolResource(school)
                .setMaxLevel(1)
                .setCooldownSeconds(cooldownSeconds)
                .setAllowCrafting(false)
                .build();
        this.baseManaCost = mana;
        this.manaCostPerLevel = 0;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 0;
    }
 
     /** Older sequence spells pass cooldown, mana, then school, and supply their own cast sound. */
     protected InstantSpell(String path, double cooldownSeconds, int mana, ResourceLocation school) {
         this(path, school, cooldownSeconds, mana, null);
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
        return Optional.ofNullable(castSound);
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
    public boolean allowLooting() {
        return false;
    }

    @Override
    public boolean allowCrafting() {
        return false;
    }

     @Override
     public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
         if (level instanceof net.minecraft.server.level.ServerLevel server) cast(server, spellLevel, caster);
         super.onCast(level, spellLevel, caster, source, data);
     }
 
     protected void cast(Level level, int spellLevel, LivingEntity caster) {}
 
     protected void cast(net.minecraft.server.level.ServerLevel level, int spellLevel, LivingEntity caster) {
         cast((Level) level, spellLevel, caster);
     }
}
