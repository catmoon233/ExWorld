package com.wan.gmmod.content.spell;

import com.wan.gmmod.content.abilities.Ability;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/** Iron's spell shell around one guimi active ability. Mana and cooldown belong to the spell. */
public final class GuimiAbilitySpell extends AbstractSpell {
    private final Ability ability;
    private final ResourceLocation spellId;
    private final DefaultConfig defaultConfig;

    public GuimiAbilitySpell(Ability ability) {
        this.ability = ability;
        this.spellId = ability.getId();
        double cooldown = Math.max(0.0, ability.getCooldownTicks() / 20.0);
        this.defaultConfig = new DefaultConfig()
                .setMinRarity(SpellRarity.COMMON)
                .setSchoolResource(SchoolRegistry.ELDRITCH_RESOURCE)
                .setMaxLevel(1)
                .setCooldownSeconds(cooldown)
                .setAllowCrafting(false)
                .build();
        this.baseManaCost = Math.max(0, ability.getSpiritualityCost());
        this.manaCostPerLevel = 0;
        this.baseSpellPower = 1;
        this.spellPowerPerLevel = 0;
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
    public Optional<net.minecraft.sounds.SoundEvent> getCastFinishSound() {
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
        return List.of(Component.translatable(ability.getDescriptionKey()));
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource source, MagicData data) {
        if (!level.isClientSide() && caster instanceof Player player) {
            ability.onActivate(player);
            if (player instanceof ServerPlayer serverPlayer) {
                com.wan.gmmod.content.quest.QuestManager.report(serverPlayer, "ability", ability.getId().toString(), 1);
            }
        }
        super.onCast(level, spellLevel, caster, source, data);
    }
}