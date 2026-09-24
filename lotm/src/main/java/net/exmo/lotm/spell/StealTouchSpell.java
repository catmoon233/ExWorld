package net.exmo.lotm.spell;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import net.exmo.lotm.LotmSupport;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/** Steal one beneficial effect. 25 mana, 15s cooldown. Wheel cast aims at the crosshair. */
public final class StealTouchSpell extends InstantSpell {
    public static final double RANGE = 5.0;

    public StealTouchSpell() {
        super("steal_touch", SchoolRegistry.ELDRITCH_RESOURCE, 15.0, 25, SoundEvents.ILLUSIONER_CAST_SPELL);
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        LivingEntity target = LotmSupport.lookedAt(entity, RANGE);
        return target != null && !beneficial(target).isEmpty();
    }

    @Override
    protected void cast(Level level, int spellLevel, LivingEntity caster) {
        if (!(level instanceof ServerLevel server)) return;
        LivingEntity target = LotmSupport.lookedAt(caster, RANGE);
        if (target != null) steal(server, caster, target);
    }

    public static MobEffectInstance steal(ServerLevel level, LivingEntity caster, LivingEntity target) {
        List<MobEffectInstance> buffs = beneficial(target);
        if (buffs.isEmpty() || caster == null || target == null || target == caster) return null;
        MobEffectInstance stolen = buffs.get(caster.getRandom().nextInt(buffs.size()));
        Holder<MobEffect> effect = stolen.getEffect();
        target.removeEffect(effect);
        MobEffectInstance current = caster.getEffect(effect);
        boolean upgrade = current == null
                || stolen.getAmplifier() > current.getAmplifier()
                || (stolen.getAmplifier() == current.getAmplifier() && stolen.getDuration() > current.getDuration());
        if (upgrade) {
            caster.removeEffect(effect);
            caster.addEffect(new MobEffectInstance(stolen));
        }
        level.playSound(null, caster.blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 0.8F, 1.2F);
        if (caster instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable("spell.lotm.steal_touch.stolen", effect.value().getDisplayName()), true);
        }
        return stolen;
    }

    public static List<MobEffectInstance> beneficial(LivingEntity target) {
        List<MobEffectInstance> buffs = new ArrayList<>();
        if (target == null) return buffs;
        for (MobEffectInstance instance : target.getActiveEffects()) {
            if (instance.getDuration() == 0) continue;
            if (instance.getEffect().value().isBeneficial()) buffs.add(instance);
        }
        return buffs;
    }
}
