package net.exmo.lotm;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.exmo.lotm.sequence.SequenceService;
import net.exmo.lotm.sequence.SequenceSkill;
import net.exmo.lotm.sequence.SkillKind;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Shared targeting and relation checks for sequence spells. Casting aims at the crosshair. */
public final class LotmSupport {
    public static final double LOOK_RANGE = 12.0;

    private LotmSupport() {}

    public static ResourceLocation attribute(Holder<Attribute> holder) {
        return holder.unwrapKey().orElseThrow().location();
    }

    public static ResourceLocation spellPower() {
        return AttributeRegistry.SPELL_POWER.getKey().location();
    }

    public static boolean hasPassive(Player player, ResourceLocation passiveId) {
        if (player == null || passiveId == null) return false;
        for (SequenceSkill skill : SequenceService.unlockedSkills(player)) {
            if (skill.kind() == SkillKind.PASSIVE && passiveId.equals(skill.ref())) return true;
        }
        return false;
    }

    public static LivingEntity lookedAt(LivingEntity caster, double range) {
        if (caster == null) return null;
        Vec3 eye = caster.getEyePosition();
        Vec3 end = eye.add(caster.getLookAngle().scale(range));
        AABB box = caster.getBoundingBox().expandTowards(caster.getLookAngle().scale(range)).inflate(1.0);
        LivingEntity best = null;
        double bestDistance = range * range;
        for (LivingEntity candidate : caster.level().getEntitiesOfClass(LivingEntity.class, box,
                living -> living != caster && living.isAlive() && living.isPickable())) {
            var hit = candidate.getBoundingBox().inflate(candidate.getPickRadius()).clip(eye, end);
            if (hit.isEmpty()) continue;
            double distance = eye.distanceToSqr(hit.get());
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        return best;
    }

    /** Entity under the crosshair, otherwise the block the look ray hits. */
    public static Vec3 lookedAtPoint(LivingEntity caster, double range) {
        Vec3 eye = caster.getEyePosition();
        Vec3 end = eye.add(caster.getLookAngle().scale(range));
        LivingEntity living = lookedAt(caster, range);
        BlockHitResult block = caster.level().clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, caster));
        if (living != null) {
            double entityDistance = eye.distanceToSqr(living.getBoundingBox().getCenter());
            if (block.getType() == HitResult.Type.MISS || entityDistance <= eye.distanceToSqr(block.getLocation())) {
                return living.position();
            }
        }
        if (block.getType() != HitResult.Type.MISS) return block.getLocation();
        return end;
    }

    public static void tell(LivingEntity caster, String key, Object... args) {
        if (caster instanceof Player player && !player.level().isClientSide()) {
            player.displayClientMessage(Component.translatable(key, args), true);
        }
    }

    public static void chat(LivingEntity caster, Component message) {
        if (caster instanceof Player player && !player.level().isClientSide()) {
            player.displayClientMessage(message, false);
        }
    }

    public static boolean undead(LivingEntity entity) {
        return entity != null && entity.getType().is(EntityTypeTags.UNDEAD);
    }

    /** Self, scoreboard allies, and owned pets. Not every passive animal. */
    public static boolean allied(LivingEntity caster, LivingEntity other) {
        if (caster == null || other == null) return false;
        if (other == caster) return true;
        if (other.isAlliedTo(caster)) return true;
        return other instanceof TamableAnimal pet && pet.getOwner() == caster;
    }

    /** Allies plus non-hostile creatures. Used by hymn and subtle guidance. */
    public static boolean friendly(LivingEntity caster, LivingEntity other) {
        if (allied(caster, other)) return true;
        return other != null && !hostile(other);
    }

    public static boolean hostile(LivingEntity entity) {
        if (entity == null || entity instanceof Player) return false;
        if (entity instanceof Enemy || entity instanceof Monster) return true;
        return entity instanceof Mob mob && mob.getTarget() != null && mob.getTarget().isAlive();
    }

    public static boolean hasHarmful(LivingEntity entity) {
        for (MobEffectInstance instance : entity.getActiveEffects()) {
            if (instance.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) return true;
        }
        return false;
    }

    public static List<Holder<MobEffect>> harmfulEffects(LivingEntity entity) {
        List<Holder<MobEffect>> harmful = new ArrayList<>();
        for (MobEffectInstance instance : entity.getActiveEffects()) {
            if (instance.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                harmful.add(instance.getEffect());
            }
        }
        return harmful;
    }
}
