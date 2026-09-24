package net.exmo.lotm.spell;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import net.exmo.lotm.LotmSupport;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Read the looked-at creature's current intent into chat. 10 mana, 5s cooldown. */
public final class MindReadSpell extends AimedSpell {
    public static final double RANGE = 12.0;

    public MindReadSpell() {
        super("mind_read", 5.0, 10, SchoolRegistry.ELDRITCH_RESOURCE, SoundEvents.AMETHYST_BLOCK_CHIME,
                "spell.lotm.mind_read.guide");
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        if (LotmTargeting.living(entity, RANGE) == null) {
            LotmSupport.tell(entity, "spell.lotm.need_creature");
            return false;
        }
        return true;
    }

    @Override
    protected void cast(ServerLevel level, int spellLevel, LivingEntity caster) {
        LivingEntity target = LotmTargeting.living(caster, RANGE);
        if (target == null) return;
        LotmSupport.chat(caster, describe(target));
    }

    static Component describe(LivingEntity target) {
        if (target instanceof Player player) {
            ItemStack held = player.getMainHandItem();
            Component item = held.isEmpty()
                    ? Component.translatable("spell.lotm.mind_read.empty_hand")
                    : held.getHoverName();
            List<String> buffs = new ArrayList<>();
            for (MobEffectInstance instance : player.getActiveEffects()) {
                if (instance.getEffect().value().getCategory() != MobEffectCategory.BENEFICIAL) continue;
                buffs.add(instance.getEffect().value().getDisplayName().getString());
                if (buffs.size() == 3) break;
            }
            Component effects = buffs.isEmpty()
                    ? Component.translatable("spell.lotm.mind_read.no_buffs")
                    : Component.literal(String.join("、", buffs));
            return Component.translatable("spell.lotm.mind_read.player", player.getName(), item, effects);
        }
        if (target instanceof Animal animal && animal.isInLove()) {
            return Component.translatable("spell.lotm.mind_read.breeding", target.getName());
        }
        if (target.isSleeping() || (target instanceof Villager villager && villager.getBrain().isActive(Activity.REST))) {
            return Component.translatable("spell.lotm.mind_read.sleeping", target.getName());
        }
        if (target instanceof Mob mob) {
            if (target instanceof Villager villager && villager.getBrain().isActive(Activity.PANIC)) {
                return Component.translatable("spell.lotm.mind_read.fleeing", target.getName());
            }
            LivingEntity chase = mob.getTarget();
            if (chase != null && chase.isAlive()) {
                return Component.translatable("spell.lotm.mind_read.chasing", target.getName(), chase.getName());
            }
            if (fleeing(mob)) return Component.translatable("spell.lotm.mind_read.fleeing", target.getName());
            if (mob.getNavigation().isInProgress()) {
                return Component.translatable("spell.lotm.mind_read.wandering", target.getName());
            }
        }
        return Component.translatable("spell.lotm.mind_read.idle", target.getName());
    }

    private static boolean fleeing(Mob mob) {
        LivingEntity hurtBy = mob.getLastHurtByMob();
        if (hurtBy == null || mob.getTarget() != null) return false;
        int since = mob.tickCount - mob.getLastHurtByMobTimestamp();
        if (since < 0 || since > 80) return false;
        Vec3 away = mob.position().subtract(hurtBy.position());
        Vec3 motion = mob.getDeltaMovement();
        return motion.horizontalDistanceSqr() > 0.002 && motion.dot(away) > 0.0;
    }
}
