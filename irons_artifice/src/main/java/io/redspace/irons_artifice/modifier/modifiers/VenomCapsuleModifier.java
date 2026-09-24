package io.redspace.irons_artifice.modifier.modifiers;

import io.redspace.irons_artifice.client.particle.ColorTransitionParticleOption;
import io.redspace.irons_artifice.data.PlayableSound;
import io.redspace.irons_artifice.data.ShotComponentMap;
import io.redspace.irons_artifice.data.ShotComponents;
import io.redspace.irons_artifice.modifier.GunModifier;
import io.redspace.irons_artifice.modifier.on_hit_handlers.PoisonPostHit;
import io.redspace.irons_artifice.modifier.on_hit_handlers.VenomOnHit;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.function.Consumer;

public final class VenomCapsuleModifier implements GunModifier {
    public static final int POISON_TICKS_PER = 5 * 20;
    public static final int AMP_PER = 1;

    @Override
    public void apply(ShotComponentMap components) {
        PoisonPostHit postHit = components.getOrCreate(ShotComponents.POST_HIT_EFFECTS)
                .getOrCreate(PoisonPostHit.class, () -> new PoisonPostHit(0, -1));
        postHit.addDuration(POISON_TICKS_PER);
        postHit.addAmplifier(1);

        VenomOnHit onHit = components.getOrCreate(ShotComponents.ON_HIT)
                .getOrCreate(VenomOnHit.class, () -> new VenomOnHit(0, -1));
        onHit.addDuration(POISON_TICKS_PER);
        onHit.addAmplifier(AMP_PER);

        components.getOrCreate(ShotComponents.PARTICLE_TRAIL).add(ColorTransitionParticleOption.bulletTrail(0x87a363, 0x04f0b00));
        components.getOrCreate(ShotComponents.IMPACT_SOUND).addGenericAccent(PlayableSound.of(PlayableSound.holder(SoundEvents.SPLASH_POTION_BREAK), 1f, 0.9f, 1.1f));
    }

    @Override
    public void getDescriptionText(Consumer<Component> builder) {
        builder.accept(Component.translatable("irons_artifice.modifier.venom_capsule").withStyle(ChatFormatting.AQUA));
        builder.accept(Component.translatable("irons_artifice.component_type.poison_duration_on_hit", POISON_TICKS_PER / 20).withStyle(ChatFormatting.GREEN));
        builder.accept(Component.translatable("irons_artifice.component_type.poison_amplifier_on_hit", AMP_PER).withStyle(ChatFormatting.GREEN));
    }
}