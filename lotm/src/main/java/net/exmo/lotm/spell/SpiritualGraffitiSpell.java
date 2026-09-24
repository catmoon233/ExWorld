package net.exmo.lotm.spell;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import net.exmo.lotm.graffiti.GraffitiTraps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/** Place a graffiti trap. 15 mana, 20s cooldown. */
public final class SpiritualGraffitiSpell extends InstantSpell {
    public SpiritualGraffitiSpell() {
        super("spiritual_graffiti", SchoolRegistry.ELDRITCH_RESOURCE, 20.0, 15, SoundEvents.SLIME_BLOCK_PLACE);
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundEvents.SLIME_BLOCK_PLACE);
    }

    @Override
    protected void cast(Level level, int spellLevel, LivingEntity caster) {
        if (!(level instanceof ServerLevel server)) return;
        Vec3 eye = caster.getEyePosition();
        Vec3 end = eye.add(caster.getLookAngle().scale(6.0));
        BlockHitResult hit = server.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, caster));
        var pos = hit.getType() == HitResult.Type.BLOCK
                ? hit.getBlockPos().relative(hit.getDirection())
                : caster.blockPosition();
        GraffitiTraps.place(server, pos, caster.getUUID());
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("spell.lotm.spiritual_graffiti.guide"));
    }
}
