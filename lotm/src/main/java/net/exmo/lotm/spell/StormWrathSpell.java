package net.exmo.lotm.spell;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/** Forward physical cone. Damage rises as health falls. 20 mana, 10s cooldown. */
public final class StormWrathSpell extends ConeStrikeSpell {
    public StormWrathSpell() {
        super("storm_wrath", 10.0, 20, 6, SchoolRegistry.LIGHTNING_RESOURCE);
    }

    @Override
    protected net.minecraft.world.damagesource.DamageSource damageSource(LivingEntity caster) {
        if (caster instanceof Player player) return caster.damageSources().playerAttack(player);
        return super.damageSource(caster);
    }

    @Override
    protected double range() {
        return 3.8;
    }

    @Override
    protected double coneDot() {
        return 0.2;
    }

    @Override
    protected double knockback() {
        return 0.9;
    }

    @Override
    protected float damageFor(int spellLevel, LivingEntity caster) {
        float missing = 1.0F - Mth.clamp(caster.getHealth() / Math.max(1.0F, caster.getMaxHealth()), 0.0F, 1.0F);
        float base = 6.0F + (float) caster.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.45F;
        return base * (1.0F + missing * 1.5F);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("spell.lotm.storm_wrath.guide"));
    }
}
