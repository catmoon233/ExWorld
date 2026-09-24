package net.exmo.lotm.spell;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import net.exmo.lotm.LotmSupport;
import net.exmo.lotm.effect.LotmEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/** Blindness plus scrambled movement. Damage clears the scramble and leaves the blindness. */
public final class MindInterferenceSpell extends InstantSpell {
    public static final double RANGE = 8.0;
    public static final int DURATION = 80;

    public MindInterferenceSpell() {
        super("mind_interference", 25.0, 25, SchoolRegistry.ELDRITCH_RESOURCE);
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.ILLUSIONER_PREPARE_BLINDNESS);
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        LivingEntity target = LotmSupport.lookedAt(entity, RANGE);
        if (target != null && !LotmSupport.allied(entity, target)) return true;
        LotmSupport.tell(entity, "spell.lotm.mind_interference.no_target");
        return false;
    }

    @Override
    protected void cast(ServerLevel level, int spellLevel, LivingEntity caster) {
        LivingEntity target = LotmSupport.lookedAt(caster, RANGE);
        if (target == null || LotmSupport.allied(caster, target)) return;
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, DURATION, 0, false, true, true));
        target.addEffect(new MobEffectInstance(LotmEffects.MIND_SCRAMBLE, DURATION, 0, false, true, true));
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("spell.lotm.mind_interference.guide"));
    }
}
