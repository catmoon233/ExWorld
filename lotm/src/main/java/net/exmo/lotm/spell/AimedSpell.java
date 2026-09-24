package net.exmo.lotm.spell;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/** Instant sequence spell. Independent of the shared InstantSpell constructor, which is still moving. */
public abstract class AimedSpell extends AbstractSpell {
    private final ResourceLocation spellId;
    private final DefaultConfig defaultConfig;
    private final SoundEvent castSound;
    private final String guideKey;

    protected AimedSpell(String path, double cooldownSeconds, int mana, ResourceLocation school, SoundEvent castSound, String guideKey) {
        this.spellId = ResourceLocation.fromNamespaceAndPath("lotm", path);
        this.guideKey = guideKey;
        this.defaultConfig = new DefaultConfig()
                .setMinRarity(SpellRarity.COMMON)
                .setSchoolResource(school)
                .setMaxLevel(2)
                .setCooldownSeconds(cooldownSeconds)
                .setAllowCrafting(false)
                .build();
        this.baseManaCost = mana;
        this.manaCostPerLevel = 0;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 0;
        this.castSound = castSound;
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
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable(guideKey));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
        if (caster instanceof Player player) player.swing(InteractionHand.MAIN_HAND, true);
        if (level instanceof ServerLevel server) cast(server, spellLevel, caster);
        super.onCast(level, spellLevel, caster, source, data);
    }

    protected abstract void cast(ServerLevel level, int spellLevel, LivingEntity caster);
}
