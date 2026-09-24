package net.exmo.lotm.spell;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.List;

public final class WeaponBreakSpell extends ConeStrikeSpell {
    public WeaponBreakSpell() {
        super("weapon_break", 8.0, 35, 10);
    }

    @Override
    protected double range() {
        return 4.2;
    }

    @Override
    protected double coneDot() {
        return 0.15;
    }

    @Override
    protected double knockback() {
        return 0.7;
    }

    @Override
    protected float damageFor(int spellLevel, LivingEntity caster) {
        return 6.0F + getSpellPower(spellLevel, caster) * 0.5F + (float) caster.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.65F;
    }

    @Override
    protected void onHit(LivingEntity caster, LivingEntity target, int spellLevel) {
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("spell.lotm.weapon_break.guide"));
    }
}
