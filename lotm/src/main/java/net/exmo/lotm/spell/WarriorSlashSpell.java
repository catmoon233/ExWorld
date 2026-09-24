package net.exmo.lotm.spell;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.List;

public final class WarriorSlashSpell extends ConeStrikeSpell {
    public WarriorSlashSpell() {
        super("warrior_slash", 4.0, 15, 6);
    }

    @Override
    protected float damageFor(int spellLevel, LivingEntity caster) {
        return 4.0F + getSpellPower(spellLevel, caster) * 0.35F + (float) caster.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.35F;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("spell.lotm.warrior_slash.guide"));
    }
}
