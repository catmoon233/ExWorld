package net.exmo.lotm.curios;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;

/** Server-only worn effects. Cooldowns and timers stay off the item stack so Curios does not re-equip every tick. */
public final class CurioBehaviors {
    private CurioBehaviors() {}

    public static void moonDew(SlotContext context, ItemStack stack) {
        LivingEntity entity = serverWearer(context);
        if (entity == null || entity.tickCount <= 0 || entity.tickCount % 600 != 0) return;
        entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0, false, true, true));
    }

    public static void seerSight(SlotContext context, ItemStack stack) {
        LivingEntity entity = serverWearer(context);
        if (entity == null) return;
        MobEffectInstance vision = entity.getEffect(MobEffects.NIGHT_VISION);
        if (vision == null || vision.getDuration() < 220) {
            entity.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, false, false, true));
        }
    }

    public static void sleepless(SlotContext context, ItemStack stack) {
        LivingEntity entity = serverWearer(context);
        if (entity == null || entity.tickCount % 20 != 0) return;
        if (entity.hasEffect(MobEffects.BLINDNESS)) entity.removeEffect(MobEffects.BLINDNESS);
        if (entity.hasEffect(MobEffects.DARKNESS)) entity.removeEffect(MobEffects.DARKNESS);
    }

    public static void sailor(SlotContext context, ItemStack stack) {
        LivingEntity entity = serverWearer(context);
        if (entity == null || !(entity.isInWater() || entity.isUnderWater())) return;
        MobEffectInstance grace = entity.getEffect(MobEffects.DOLPHINS_GRACE);
        if (grace == null || grace.getDuration() < 20) {
            entity.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 80, 0, false, false, true));
        }
    }

    public static void conceal(SlotContext context, ItemStack stack) {
        LivingEntity entity = serverWearer(context);
        if (entity == null || !entity.isShiftKeyDown()) return;
        MobEffectInstance invisibility = entity.getEffect(MobEffects.INVISIBILITY);
        if (invisibility == null || invisibility.getDuration() < 15) {
            entity.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false, true));
        }
    }

    public static void fatePulse(SlotContext context, ItemStack stack) {
        LivingEntity entity = serverWearer(context);
        if (entity == null || entity.tickCount <= 0 || entity.tickCount % 1200 != 0) return;
        entity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 0, false, true, true));
    }

    private static LivingEntity serverWearer(SlotContext context) {
        if (context == null || context.cosmetic() || context.entity() == null) return null;
        if (context.entity().level().isClientSide()) return null;
        return context.entity();
    }
}
