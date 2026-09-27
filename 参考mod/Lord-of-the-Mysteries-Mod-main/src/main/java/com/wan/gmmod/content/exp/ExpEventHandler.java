package com.wan.gmmod.content.exp;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.abilities.SkillManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * 实验性途径的事件驱动机制：受击减伤 / 闪避 / 格挡、攻击加成 / 吸血、
 * 暴怒叠层，以及临时召唤物的到期消散与目标维护。
 * <p>
 * 所有分支都以 {@link SkillManager#isUnlocked} 判定入口——实验开关关闭时
 * 该方法返回空集，本类逻辑自动全部失效，无需单独门控。
 * <p>
 * 层数 / 内置冷却等轻量临时状态存于 {@link Entity#getPersistentData()}，不新增附件。
 */
@EventBusSubscriber(modid = GuimiMod.MODID)
public final class ExpEventHandler {

    // 暴怒之民「暴怒」：层数 / 最后战斗时刻
    private static final String RAGE_STACKS = "gmmod_exp_rage";
    private static final String RAGE_TIME = "gmmod_exp_rage_time";
    /** 脱战 30 秒后暴怒层数重置 */
    private static final int RAGE_RESET_TICKS = 600;
    // 格斗家「格挡反击」内置冷却结束时刻
    private static final String BLOCK_CD = "gmmod_exp_block_cd";
    // 格斗家「连击」：目标 / 层数 / 最后命中时刻
    private static final String COMBO_TARGET = "gmmod_exp_combo_target";
    private static final String COMBO_STACKS = "gmmod_exp_combo";
    private static final String COMBO_TIME = "gmmod_exp_combo_time";

    private ExpEventHandler() {}

    /** 玩家受击：闪避 / 减伤 / 格挡反击 / 暴怒叠层。 */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer victim) {
            handleVictim(event, victim);
        }
        if (event.getSource().getEntity() instanceof ServerPlayer attacker
                && event.getEntity() != attacker) {
            handleAttacker(event, attacker);
        }
    }

    private static void handleVictim(LivingIncomingDamageEvent event, ServerPlayer sp) {
        CompoundTag data = sp.getPersistentData();
        long now = sp.serverLevel().getGameTime();
        float amount = event.getAmount();
        boolean physical = event.getSource().getDirectEntity() != null
                && !event.getSource().is(DamageTypeTags.IS_FIRE);

        // 水手「幻鳞」：受攻击 20% 概率滑开，伤害减半
        if (SkillManager.isUnlocked(sp, GuimiMod.id("tyr_phantom_scales"))
                && physical && sp.getRandom().nextFloat() < 0.20F) {
            amount *= 0.5F;
            ExpFx.burst(sp.serverLevel(), sp, ParticleTypes.BUBBLE_POP, 12);
        }
        // 格斗家「超凡抗性」：伤害减免 +10%
        if (SkillManager.isUnlocked(sp, GuimiMod.id("gia_transcendent_resist"))) {
            amount *= 0.90F;
        }
        // 冷血者「非人之躯」：物理伤害 -15%
        if (physical && SkillManager.isUnlocked(sp, GuimiMod.id("aby_inhuman_body"))) {
            amount *= 0.85F;
        }
        // 活尸「钢铁之躯」：物理伤害 -30%，免疫火焰 / 溺水
        if (SkillManager.isUnlocked(sp, GuimiMod.id("cha_steel_body"))) {
            if (event.getSource().is(DamageTypeTags.IS_FIRE)
                    || event.getSource().is(DamageTypeTags.IS_DROWNING)) {
                event.setCanceled(true);
                return;
            }
            if (physical) amount *= 0.70F;
        }
        // 格斗家「格挡反击」：潜行受击时完美格挡并反击 ×1.5（内置冷却 8 秒）
        if (SkillManager.isUnlocked(sp, GuimiMod.id("gia_perfect_block"))
                && sp.isShiftKeyDown() && physical && now >= data.getLong(BLOCK_CD)) {
            data.putLong(BLOCK_CD, now + 160);
            event.setCanceled(true);
            if (event.getSource().getEntity() instanceof LivingEntity attacker) {
                attacker.hurt(sp.damageSources().playerAttack(sp), event.getAmount() * 1.5F);
            }
            sp.serverLevel().playSound(null, sp.blockPosition(),
                    net.minecraft.sounds.SoundEvents.SHIELD_BLOCK,
                    net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.2F);
            return;
        }
        // 暴怒之民「暴怒」：受伤叠层（上限 10），脱战 30 秒重置
        if (SkillManager.isUnlocked(sp, GuimiMod.id("tyr_rage"))) {
            int stacks = now - data.getLong(RAGE_TIME) > RAGE_RESET_TICKS ? 0 : data.getInt(RAGE_STACKS);
            data.putInt(RAGE_STACKS, Math.min(10, stacks + 1));
            data.putLong(RAGE_TIME, now);
        }
        event.setAmount(amount);
    }

    private static void handleAttacker(LivingIncomingDamageEvent event, ServerPlayer sp) {
        CompoundTag data = sp.getPersistentData();
        long now = sp.serverLevel().getGameTime();
        float amount = event.getAmount();

        // 暴怒之民「暴怒」：每层 +3% 伤害
        if (SkillManager.isUnlocked(sp, GuimiMod.id("tyr_rage"))) {
            int stacks = now - data.getLong(RAGE_TIME) > RAGE_RESET_TICKS ? 0 : data.getInt(RAGE_STACKS);
            if (stacks > 0) {
                amount *= 1.0F + stacks * 0.03F;
                data.putLong(RAGE_TIME, now);
            }
        }
        // 格斗家「连击」：3 秒内连续命中同一目标，每次 +10%（至多 3 层）
        if (SkillManager.isUnlocked(sp, GuimiMod.id("gia_combo"))) {
            int targetId = event.getEntity().getId();
            boolean chain = data.getInt(COMBO_TARGET) == targetId
                    && now - data.getLong(COMBO_TIME) <= 60;
            int stacks = chain ? Math.min(3, data.getInt(COMBO_STACKS) + 1) : 0;
            data.putInt(COMBO_TARGET, targetId);
            data.putInt(COMBO_STACKS, stacks);
            data.putLong(COMBO_TIME, now);
            amount *= 1.0F + stacks * 0.10F;
        }
        // 吸血鬼「黑夜之力」：夜间伤害 +20%，白天 -15%
        if (SkillManager.isUnlocked(sp, GuimiMod.id("moon_night_power"))) {
            amount *= sp.serverLevel().isDay() ? 0.85F : 1.20F;
        }
        // 吸血鬼「腐蚀之爪」：近战命中附加 2 点腐蚀伤害（破防）
        if (event.getSource().getDirectEntity() == sp
                && SkillManager.isUnlocked(sp, GuimiMod.id("moon_corrosion_claw"))) {
            amount += 2.0F;
            ExpFx.burst(sp.serverLevel(), event.getEntity(), ParticleTypes.SMOKE, 6);
        }
        event.setAmount(amount);
    }

    /** 攻击结算后：吸血尖牙按实际伤害回血。 */
    @SubscribeEvent
    public static void onDamagePost(LivingDamageEvent.Post event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer sp) || event.getEntity() == sp) {
            return;
        }
        // 吸血鬼「吸血尖牙」：近战命中恢复伤害 15% 的生命
        if (event.getSource().getDirectEntity() == sp
                && SkillManager.isUnlocked(sp, GuimiMod.id("moon_vampire_fangs"))) {
            sp.heal(event.getNewDamage() * 0.15F);
            ExpFx.burst(sp.serverLevel(), sp, ParticleTypes.DAMAGE_INDICATOR, 4);
        }
    }

    /** 临时召唤物：到期消散；友方召唤物不咬主人并自动协战。 */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide) return;
        ServerLevel level = (ServerLevel) mob.level();
        CompoundTag data = mob.getPersistentData();

        // 血仆转化：到期恢复原状；期间跟随并攻击主人攻击的目标
        if (mob.getTags().contains(ExpAbilityTypes.Summon.BLOOD_SERVANT_TAG)) {
            if (level.getGameTime() >= data.getLong(ExpAbilityTypes.Summon.BLOOD_EXPIRE_KEY)) {
                mob.getTags().remove(ExpAbilityTypes.Summon.BLOOD_SERVANT_TAG);
                data.remove(ExpAbilityTypes.Summon.BLOOD_EXPIRE_KEY);
                data.remove(ExpAbilityTypes.Summon.OWNER_KEY);
                mob.setTarget(null);
                ExpFx.burst(level, mob, ParticleTypes.SMOKE, 10);
                return;
            }
            Player owner = data.hasUUID(ExpAbilityTypes.Summon.OWNER_KEY)
                    ? level.getPlayerByUUID(data.getUUID(ExpAbilityTypes.Summon.OWNER_KEY)) : null;
            if (owner == null) return;
            if (mob.getTarget() == owner) {
                mob.setTarget(null);
            }
            if (mob.tickCount % 20 == 0) {
                LivingEntity attack = owner.getLastHurtMob();
                if (attack == null || !attack.isAlive() || owner.distanceToSqr(attack) > 576) {
                    attack = nearestEnemy(owner, mob, level);
                }
                if (attack != null) mob.setTarget(attack);
            }
            return;
        }

        if (!mob.getTags().contains(ExpAbilityTypes.Summon.SUMMON_TAG)) return;

        // 到期消散
        if (level.getGameTime() >= data.getLong(ExpAbilityTypes.Summon.DESPAWN_KEY)) {
            ExpFx.burst(level, mob, ParticleTypes.LARGE_SMOKE, 20);
            mob.discard();
            return;
        }
        // 敌对个体（召唤失控）不做目标维护
        if (data.getBoolean(ExpAbilityTypes.Summon.HOSTILE_KEY)) return;

        Player owner = data.hasUUID(ExpAbilityTypes.Summon.OWNER_KEY)
                ? level.getPlayerByUUID(data.getUUID(ExpAbilityTypes.Summon.OWNER_KEY)) : null;
        if (owner == null) return;
        // 友方召唤物：不攻击主人；周期性锁定主人附近最近的敌对生物
        if (mob.getTarget() == owner) {
            mob.setTarget(null);
        }
        if (mob.tickCount % 40 == 0 && (mob.getTarget() == null || !mob.getTarget().isAlive())) {
            LivingEntity nearest = nearestEnemy(owner, mob, level);
            if (nearest != null) mob.setTarget(nearest);
        }
    }

    /** 主人附近 16 米内最近的敌对生物（不含自身与友方召唤物）。 */
    private static LivingEntity nearestEnemy(Player owner, Mob self, ServerLevel level) {
        return level.getEntitiesOfClass(LivingEntity.class,
                        owner.getBoundingBox().inflate(16),
                        e -> e instanceof Enemy && e.isAlive() && e != self
                                && !e.getTags().contains(ExpAbilityTypes.Summon.SUMMON_TAG))
                .stream().min(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(self)))
                .orElse(null);
    }
}
