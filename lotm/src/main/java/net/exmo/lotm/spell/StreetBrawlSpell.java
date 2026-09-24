package net.exmo.lotm.spell;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.List;

/** One quick blow that hits harder than a jab and slows the target. 10 mana, 6s cooldown. */
public final class StreetBrawlSpell extends ConeStrikeSpell {
    public StreetBrawlSpell() {
        super("street_brawl", 6.0, 10, 6);
    }

    @Override
    protected double range() {
        return 3.0;
    }

    @Override
    protected double coneDot() {
        return 0.75;
    }

    @Override
    protected double knockback() {
        return 0.25;
    }

    @Override
    protected float damageFor(int spellLevel, LivingEntity caster) {
        return 4.0F + getSpellPower(spellLevel, caster) * 0.25F
                + (float) caster.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.55F;
    }

    @Override
    protected void onHit(LivingEntity caster, LivingEntity target, int spellLevel) {
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("spell.lotm.street_brawl.guide"));
    }
}
