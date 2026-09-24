package net.exmo.lotm.spell;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.List;

public final class PugilistComboSpell extends ConeStrikeSpell {
    public PugilistComboSpell() {
        super("pugilist_combo", 6.0, 25, 8);
    }

    @Override
    protected double range() {
        return 2.6;
    }

    @Override
    protected double coneDot() {
        return 0.2;
    }

    @Override
    protected double knockback() {
        return 0.55;
    }

    @Override
    protected float damageFor(int spellLevel, LivingEntity caster) {
        return 5.0F + getSpellPower(spellLevel, caster) * 0.45F + (float) caster.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.5F;
    }

    @Override
    protected void onHit(LivingEntity caster, LivingEntity target, int spellLevel) {
        target.hurt(getDamageSource(caster), damageFor(spellLevel, caster) * 0.65F);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("spell.lotm.pugilist_combo.guide"));
    }
}
