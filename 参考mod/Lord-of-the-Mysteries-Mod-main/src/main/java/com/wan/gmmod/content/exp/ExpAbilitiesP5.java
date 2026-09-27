package com.wan.gmmod.content.exp;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.common.network.packet.StealMenuPacket;
import com.wan.gmmod.common.registry.ModEffects;
import com.wan.gmmod.content.abilities.Ability;
import com.wan.gmmod.content.abilities.AbilityRegistry;
import com.wan.gmmod.content.abilities.SkillManager;
import com.wan.gmmod.content.exp.ExpAbilityTypes.Aoe;
import com.wan.gmmod.content.exp.ExpAbilityTypes.Custom;
import com.wan.gmmod.content.exp.ExpAbilityTypes.Marker;
import com.wan.gmmod.content.exp.ExpAbilityTypes.PassiveEffect;
import com.wan.gmmod.content.exp.ExpAbilityTypes.SelfBuff;
import com.wan.gmmod.content.exp.ExpAbilityTypes.Target;
import com.wan.gmmod.content.exp.ExpAbilityTypes.Teleport;
import com.wan.gmmod.content.exp.ExpAbilityTypes.TickPassive;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import net.neoforged.neoforge.network.PacketDistributor;

import static com.wan.gmmod.content.exp.ExpFx.fx;

/**
 * 实验性途径能力注册 · 第五组：月亮(moon_) / 母亲(mot_) / 错误(err_) / 门(door_)。
 */
final class ExpAbilitiesP5 {

    private ExpAbilitiesP5() {}

    private static void reg(String seq, Ability ability) {
        AbilityRegistry.register(GuimiMod.id(seq), ability);
    }

    /** 对玩家附近的可催熟方块（庄稼 / 树苗等）随机施加催熟。 */
    private static void bonemealAround(ServerPlayer sp, int radius, int attempts) {
        ServerLevel level = sp.serverLevel();
        for (int i = 0; i < attempts; i++) {
            BlockPos pos = sp.blockPosition().offset(
                    level.random.nextInt(radius * 2 + 1) - radius,
                    level.random.nextInt(3) - 1,
                    level.random.nextInt(radius * 2 + 1) - radius);
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof BonemealableBlock grow
                    && grow.isValidBonemealTarget(level, pos, state)) {
                grow.performBonemeal(level, level.random, pos, state);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 5, 0.3, 0.3, 0.3, 0.0);
            }
        }
    }

    static void init() {
        // ===== 月亮 · 序列9 药师 =====
        reg("moon_9", new Marker("moon_potion_boost"));
        // 毒性抵抗：中毒效果快速消退
        reg("moon_9", new TickPassive("moon_poison_resist", 40, sp -> {
            if (sp.hasEffect(MobEffects.POISON) && sp.getRandom().nextFloat() < 0.5F) {
                sp.removeEffect(MobEffects.POISON);
            }
        }));
        reg("moon_9", new Marker("moon_herb_identify"));

        // ===== 月亮 · 序列8 驯兽师 =====
        reg("moon_8", new Marker("moon_animal_affinity"));
        reg("moon_8", new Marker("moon_tame_master"));
        // 兽语：命令 30 米内自己驯服的动物攻击最近袭击自己的目标
        reg("moon_8", new Custom("moon_beast_speech", 3, 5, sp -> {
            LivingEntity enemy = sp.getLastHurtByMob();
            int commanded = 0;
            for (LivingEntity e : ExpFx.around(sp, 30, ExpFx.Filter.ALL)) {
                if (e instanceof TamableAnimal pet && pet.getOwner() == sp
                        && enemy != null && enemy.isAlive()) {
                    pet.setTarget(enemy);
                    commanded++;
                }
            }
            if (commanded == 0) {
                ExpFx.noTarget(sp);
                ExpFx.refund(sp, 3);
                return;
            }
            ExpFx.activated(sp, "ability.guimi_mod.moon_beast_speech");
        }));
        // 野兽召唤：号召 15 米内至多 3 只野生生物助战 60 秒
        reg("moon_8", new Custom("moon_beast_summon", 20, 120, sp -> {
            int recruited = 0;
            for (LivingEntity e : ExpFx.around(sp, 15, ExpFx.Filter.ALL)) {
                if (recruited >= 3 || !(e instanceof Mob mob)
                        || ExpFx.Filter.HOSTILE.test(sp, e) || e instanceof TamableAnimal) {
                    continue;
                }
                mob.addTag(ExpAbilityTypes.Summon.SUMMON_TAG);
                mob.getPersistentData().putLong(ExpAbilityTypes.Summon.DESPAWN_KEY,
                        sp.serverLevel().getGameTime() + Long.MAX_VALUE / 4); // 不消散，仅协战
                mob.getPersistentData().putUUID(ExpAbilityTypes.Summon.OWNER_KEY, sp.getUUID());
                mob.getPersistentData().putBoolean(ExpAbilityTypes.Summon.HOSTILE_KEY, false);
                ExpFx.burst(sp.serverLevel(), mob, ParticleTypes.HEART, 6);
                recruited++;
            }
            if (recruited == 0) {
                ExpFx.noTarget(sp);
                ExpFx.refund(sp, 20);
                return;
            }
            ExpFx.activated(sp, "ability.guimi_mod.moon_beast_summon");
        }));

        // ===== 月亮 · 序列7 吸血鬼 =====
        // 生命汲取：吸取 5 米内目标生命并等量恢复自身
        reg("moon_7", new Custom("moon_life_drain", 10, 25, sp -> {
            List<LivingEntity> hits = ExpFx.rayTargets(sp, 5, 1);
            if (hits.isEmpty()) {
                ExpFx.noTarget(sp);
                ExpFx.refund(sp, 10);
                return;
            }
            LivingEntity target = hits.get(0);
            float before = target.getHealth();
            target.hurt(sp.damageSources().indirectMagic(sp, sp), 10.0F);
            sp.heal(Math.max(0, before - target.getHealth()));
            ExpFx.burst(sp.serverLevel(), target, ParticleTypes.DAMAGE_INDICATOR, 20);
            ExpFx.activated(sp, "ability.guimi_mod.moon_life_drain");
        }));
        // 吸血尖牙 / 黑夜之力伤害部分：由 ExpEventHandler 实现
        reg("moon_7", new Marker("moon_vampire_fangs"));
        // 黑夜之力：夜间移速提升（伤害增减由事件钩子处理）
        reg("moon_7", new TickPassive("moon_night_power", 40, sp -> {
            if (!sp.serverLevel().isDay()) {
                ExpFx.apply(sp, List.of(fx(MobEffects.MOVEMENT_SPEED, 0)), 100, true);
            }
        }));
        // 蝙蝠化形：化身蝙蝠（抗性 V + 滑翔 + 跳跃 + 短暂隐身为蝙蝠群），并召唤蝙蝠群扑咬周围敌人
        reg("moon_7", new Custom("moon_bat_form", 15, 60, sp -> {
            ExpFx.apply(sp, List.of(
                    fx(MobEffects.DAMAGE_RESISTANCE, 4),
                    fx(MobEffects.SLOW_FALLING, 0),
                    fx(MobEffects.JUMP, 3),
                    fx(MobEffects.INVISIBILITY, 0),
                    fx(ModEffects.BAT_FORM, 0)), 300, false);
            ExpAbilityTypes.Summon.spawn(sp, net.minecraft.world.entity.EntityType.BAT, 6,
                    1200, 6, 2, 0F);
            ExpFx.burst(sp.serverLevel(), sp, ParticleTypes.LARGE_SMOKE, 30);
            ExpFx.activated(sp, "ability.guimi_mod.moon_bat_form");
        }));
        // 血之诱惑：魅惑 8 米内生物使其靠近（敌对目标额外受到精神压制）
        reg("moon_7", new Custom("moon_blood_lure", 10, 30, sp -> {
            int lured = 0;
            for (LivingEntity e : ExpFx.around(sp, 8, ExpFx.Filter.ALL)) {
                if (e instanceof ServerPlayer) continue; // 对玩家无效
                Vec3 pull = sp.position().subtract(e.position()).normalize().scale(0.9);
                e.setDeltaMovement(pull.x, 0.12, pull.z);
                e.hurtMarked = true;
                if (ExpFx.Filter.HOSTILE.test(sp, e)) {
                    e.hurt(sp.damageSources().indirectMagic(sp, sp), 2.0F);
                }
                ExpFx.burst(sp.serverLevel(), e, ParticleTypes.HEART, 3);
                lured++;
            }
            if (lured == 0) {
                ExpFx.noTarget(sp);
                ExpFx.refund(sp, 10);
                return;
            }
            ExpFx.activated(sp, "ability.guimi_mod.moon_blood_lure");
        }));
        // 深渊枷锁：黑暗凝成枷锁束缚 8 米内目标（10 秒高级缓慢）
        reg("moon_7", new Target("moon_abyss_shackle", 6, 15, 8, 0, 1, 10,
                fx(MobEffects.MOVEMENT_SLOWDOWN, 7)));
        // 黑暗之翼：黑雾蝠翼加速飞行，激活时蝙蝠群扑咬周围敌人
        reg("moon_7", new Custom("moon_dark_wings", 10, 40, sp -> {
            ExpFx.apply(sp, List.of(fx(MobEffects.MOVEMENT_SPEED, 2),
                    fx(MobEffects.SLOW_FALLING, 0)), 300, false);
            for (LivingEntity e : ExpFx.around(sp, 5, ExpFx.Filter.HOSTILE)) {
                e.hurt(sp.damageSources().indirectMagic(sp, sp), 5.0F);
                ExpFx.burst(sp.serverLevel(), e, ParticleTypes.SMOKE, 8);
            }
            ExpFx.burst(sp.serverLevel(), sp, ParticleTypes.ENCHANT, 20);
            ExpFx.activated(sp, "ability.guimi_mod.moon_dark_wings");
        }));
        // 腐蚀之爪：符号利爪，近战命中附加腐蚀破防（伤害由事件钩子处理）
        reg("moon_7", new Marker("moon_corrosion_claw"));
        // 血仆转化：驯化 6 米内单只生物为友方血仆 60 秒（跟随并攻击你攻击的目标）
        reg("moon_7", new Custom("moon_blood_servant", 15, 60, sp -> {
            List<LivingEntity> hits = ExpFx.rayTargets(sp, 6, 1);
            if (hits.isEmpty()) {
                ExpFx.noTarget(sp);
                ExpFx.refund(sp, 15);
                return;
            }
            LivingEntity target = hits.get(0);
            if (target instanceof ServerPlayer || !(target instanceof Mob mob)) {
                ExpFx.noTarget(sp);
                ExpFx.refund(sp, 15);
                return;
            }
            mob.addTag(ExpAbilityTypes.Summon.BLOOD_SERVANT_TAG);
            mob.getPersistentData().putLong(ExpAbilityTypes.Summon.BLOOD_EXPIRE_KEY,
                    sp.serverLevel().getGameTime() + 1200); // 60 秒
            mob.getPersistentData().putUUID(ExpAbilityTypes.Summon.OWNER_KEY, sp.getUUID());
            if (mob.getTarget() == sp) {
                mob.setTarget(null);
            }
            mob.setPersistenceRequired();
            ExpFx.burst(sp.serverLevel(), mob, ParticleTypes.SOUL, 20);
            ExpFx.activated(sp, "ability.guimi_mod.moon_blood_servant");
        }));

        // ===== 月亮 · 序列6 魔药教授 =====
        reg("moon_6", new Marker("moon_potion_mastery"));
        // 血之魔药：消耗自身 4 点生命，为目标（或自己）恢复 6 点
        reg("moon_6", new Custom("moon_blood_potion", 0, 15, sp -> {
            if (sp.getHealth() <= 4.0F) {
                ExpFx.noTarget(sp);
                return;
            }
            sp.hurt(sp.damageSources().magic(), 4.0F);
            List<LivingEntity> hits = ExpFx.rayTargets(sp, 8, 1);
            LivingEntity target = hits.isEmpty() ? sp : hits.get(0);
            target.heal(6.0F);
            ExpFx.burst(sp.serverLevel(), target, ParticleTypes.HEART, 10);
            ExpFx.activated(sp, "ability.guimi_mod.moon_blood_potion");
        }));
        reg("moon_6", new Marker("moon_toxin_master"));
        // 变异药剂：随机获得一种非凡增益 60 秒，10% 概率负面变异（负面短暂）
        reg("moon_6", new Custom("moon_mutation_potion", 20, 90, sp -> {
            if (sp.getRandom().nextFloat() < 0.10F) {
                ExpFx.apply(sp, List.of(fx(MobEffects.CONFUSION, 0), fx(MobEffects.POISON, 0)), 100, false);
            } else {
                var pool = List.of(fx(MobEffects.DAMAGE_BOOST, 1), fx(MobEffects.MOVEMENT_SPEED, 1),
                        fx(MobEffects.DAMAGE_RESISTANCE, 1), fx(MobEffects.FIRE_RESISTANCE, 0),
                        fx(MobEffects.JUMP, 2), fx(MobEffects.INVISIBILITY, 0));
                ExpFx.apply(sp, List.of(pool.get(sp.getRandom().nextInt(pool.size()))), 1200, false);
            }
            ExpFx.activated(sp, "ability.guimi_mod.moon_mutation_potion");
        }));
        // 血族再生：生命低于 50% 时缓慢回血
        reg("moon_6", new TickPassive("moon_blood_regen", 20, sp -> {
            if (sp.getHealth() < sp.getMaxHealth() * 0.5F) {
                sp.heal(1.0F);
            }
        }));

        // ===== 母亲 · 序列9 耕种者 =====
        // 丰收之手：周期性催熟身边作物
        reg("mother_9", new TickPassive("mot_harvest_hand", 100,
                sp -> bonemealAround(sp, 4, 4)));
        reg("mother_9", new Marker("mot_nature_affinity"));
        // 再生：站立不动时生命恢复加快
        reg("mother_9", new TickPassive("mot_regeneration", 40, sp -> {
            if (sp.getDeltaMovement().horizontalDistanceSqr() < 0.001) {
                ExpFx.apply(sp, List.of(fx(MobEffects.REGENERATION, 0)), 60, true);
            }
        }));

        // ===== 母亲 · 序列8 医师 =====
        reg("mother_8", new Marker("mot_medical_knowledge"));
        // 诊断：显示目标生命值等详细信息
        reg("mother_8", new Custom("mot_diagnose", 1, 3, sp -> {
            List<LivingEntity> hits = ExpFx.rayTargets(sp, 10, 1);
            if (hits.isEmpty()) {
                ExpFx.noTarget(sp);
                ExpFx.refund(sp, 1);
                return;
            }
            LivingEntity target = hits.get(0);
            sp.sendSystemMessage(Component.translatable("message.guimi_mod.exp.analyze",
                    target.getDisplayName(),
                    String.format("%.0f", target.getHealth()),
                    String.format("%.0f", target.getMaxHealth())));
        }));
        // 急救：瞬间恢复目标 4 心生命并移除流血（负面）效果
        reg("mother_8", new Custom("mot_first_aid", 10, 40, sp -> {
            List<LivingEntity> hits = ExpFx.rayTargets(sp, 8, 1);
            LivingEntity target = hits.isEmpty() ? sp : hits.get(0);
            target.heal(8.0F);
            ExpFx.clearNegative(target);
            ExpFx.burst(sp.serverLevel(), target, ParticleTypes.HEART, 12);
            ExpFx.activated(sp, "ability.guimi_mod.mot_first_aid");
        }));
        // 疾病抵抗：周期性净化中毒 / 凋零 / 饥饿
        reg("mother_8", new TickPassive("mot_disease_resist", 60, sp -> {
            sp.removeEffect(MobEffects.POISON);
            sp.removeEffect(MobEffects.WITHER);
            sp.removeEffect(MobEffects.HUNGER);
        }));

        // ===== 母亲 · 序列7 丰收祭司 =====
        // 丰收祝福：大范围催熟 + 友方生命恢复II / 饱和
        reg("mother_7", new Custom("mot_harvest_blessing", 25, 120, sp -> {
            bonemealAround(sp, 15, 60);
            List<ExpFx.Effect> blessing = List.of(fx(MobEffects.REGENERATION, 1),
                    fx(MobEffects.SATURATION, 0));
            ExpFx.apply(sp, blessing, 400, false);
            for (LivingEntity e : ExpFx.around(sp, 15, ExpFx.Filter.FRIENDLY)) {
                ExpFx.apply(e, blessing, 400, false);
            }
            ExpFx.activated(sp, "ability.guimi_mod.mot_harvest_blessing");
        }));
        // 荆棘术：荆棘丛（减速 + 持续刺伤）
        reg("mother_7", new Aoe("mot_thorns", 10, 25, 5, ExpFx.Filter.HOSTILE,
                false, 2, 0, false, 10, fx(MobEffects.MOVEMENT_SLOWDOWN, 2), fx(MobEffects.POISON, 0)));
        // 自然之愈：驱散友方负面并恢复 10 点生命
        reg("mother_7", new Custom("mot_nature_heal", 15, 45, sp -> {
            ExpFx.clearNegative(sp);
            sp.heal(10.0F);
            for (LivingEntity e : ExpFx.around(sp, 10, ExpFx.Filter.FRIENDLY)) {
                ExpFx.clearNegative(e);
                e.heal(10.0F);
                ExpFx.burst(sp.serverLevel(), e, ParticleTypes.HAPPY_VILLAGER, 10);
            }
            ExpFx.activated(sp, "ability.guimi_mod.mot_nature_heal");
        }));
        // 大地之力：站在泥土 / 草方块上时力量II + 抗性II
        reg("mother_7", new TickPassive("mot_earth_power", 40, sp -> {
            BlockState below = sp.serverLevel().getBlockState(sp.blockPosition().below());
            if (below.is(Blocks.GRASS_BLOCK) || below.is(Blocks.DIRT)
                    || below.is(Blocks.COARSE_DIRT) || below.is(Blocks.PODZOL)
                    || below.is(Blocks.FARMLAND)) {
                ExpFx.apply(sp, List.of(fx(MobEffects.DAMAGE_BOOST, 1),
                        fx(MobEffects.DAMAGE_RESISTANCE, 1)), 100, true);
            }
        }));

        // ===== 母亲 · 序列6 生物学家 =====
        // 生物改造：永久提升自己驯服动物的属性（每只一次）
        reg("mother_6", new Custom("mot_bio_modify", 30, 300, sp -> {
            List<LivingEntity> hits = ExpFx.rayTargets(sp, 8, 1);
            if (hits.isEmpty() || !(hits.get(0) instanceof TamableAnimal pet)
                    || pet.getOwner() != sp || pet.getTags().contains("gmmod_exp_modified")) {
                ExpFx.noTarget(sp);
                ExpFx.refund(sp, 30);
                return;
            }
            pet.addTag("gmmod_exp_modified");
            var health = pet.getAttribute(Attributes.MAX_HEALTH);
            if (health != null) {
                health.setBaseValue(health.getBaseValue() * 1.3);
                pet.setHealth(pet.getMaxHealth());
            }
            var attack = pet.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attack != null) {
                attack.setBaseValue(attack.getBaseValue() * 1.3);
            }
            ExpFx.burst(sp.serverLevel(), pet, ParticleTypes.HAPPY_VILLAGER, 20);
            ExpFx.activated(sp, "ability.guimi_mod.mot_bio_modify");
        }));
        // 自然之怒：植物缠绕 15 米内敌人（束缚 + 刺伤）
        reg("mother_6", new Aoe("mot_nature_wrath", 20, 60, 15, ExpFx.Filter.HOSTILE,
                false, 8, 0, false, 5, fx(MobEffects.MOVEMENT_SLOWDOWN, 4)));
        // 生命共享：与目标玩家平均分配生命值
        reg("mother_6", new Custom("mot_life_share", 20, 180, sp -> {
            List<LivingEntity> hits = ExpFx.rayTargets(sp, 10, 1);
            if (hits.isEmpty() || !(hits.get(0) instanceof ServerPlayer other)) {
                ExpFx.noTarget(sp);
                ExpFx.refund(sp, 20);
                return;
            }
            float avg = (sp.getHealth() + other.getHealth()) / 2.0F;
            sp.setHealth(Math.min(avg, sp.getMaxHealth()));
            other.setHealth(Math.min(avg, other.getMaxHealth()));
            ExpFx.burst(sp.serverLevel(), other, ParticleTypes.HEART, 15);
            ExpFx.activated(sp, "ability.guimi_mod.mot_life_share");
        }));
        // 瘟疫抵抗光环：8 米内友方免疫疾病 / 中毒 / 凋零
        reg("mother_6", new TickPassive("mot_plague_aura", 60, sp -> {
            sp.removeEffect(MobEffects.POISON);
            sp.removeEffect(MobEffects.WITHER);
            for (LivingEntity e : ExpFx.around(sp, 8, ExpFx.Filter.FRIENDLY)) {
                e.removeEffect(MobEffects.POISON);
                e.removeEffect(MobEffects.WITHER);
            }
        }));

        // ===== 错误 · 序列9 偷盗者 =====
        // 窃取：偷取 5 米内生物手持的物品
        reg("error_9", new Custom("err_steal", 5, 20, sp -> {
            List<LivingEntity> hits = ExpFx.rayTargets(sp, 5, 1);
            if (hits.isEmpty() || !(hits.get(0) instanceof Mob mob)
                    || mob.getMainHandItem().isEmpty()) {
                ExpFx.noTarget(sp);
                ExpFx.refund(sp, 5);
                return;
            }
            ItemStack loot = mob.getMainHandItem().copy();
            mob.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            sp.getInventory().placeItemBackInInventory(loot);
            ExpFx.burst(sp.serverLevel(), mob, ParticleTypes.CRIT, 8);
            ExpFx.activated(sp, "ability.guimi_mod.err_steal");
        }));
        // 敏捷之手：攻速 + 移速提升
        reg("error_9", new PassiveEffect("err_agile_hands",
                fx(MobEffects.DIG_SPEED, 0), fx(MobEffects.MOVEMENT_SPEED, 0)));
        // 卓越观察：10 米内掉落物高亮
        reg("error_9", new TickPassive("err_keen_observation", 100, sp -> {
            for (ItemEntity item : sp.serverLevel().getEntitiesOfClass(ItemEntity.class,
                    sp.getBoundingBox().inflate(10))) {
                item.setGlowingTag(true);
            }
        }));
        reg("error_9", new Marker("err_short_blade"));
        // 体质强化：生命 + 移速小幅提升
        reg("error_9", new PassiveEffect("err_constitution",
                fx(MobEffects.HEALTH_BOOST, 0), fx(MobEffects.MOVEMENT_SPEED, 0)));

        // ===== 错误 · 序列8 诈骗师 =====
        reg("error_8", new Marker("err_micro_expression"));
        reg("error_8", new Marker("err_charisma"));
        // 口才：让目标信服而放弃敌意
        reg("error_8", new Custom("err_eloquence", 5, 30, sp -> {
            List<LivingEntity> hits = ExpFx.rayTargets(sp, 10, 1);
            if (hits.isEmpty() || !(hits.get(0) instanceof Mob mob)) {
                ExpFx.noTarget(sp);
                ExpFx.refund(sp, 5);
                return;
            }
            mob.setTarget(null);
            ExpFx.burst(sp.serverLevel(), mob, ParticleTypes.HEART, 6);
            ExpFx.activated(sp, "ability.guimi_mod.err_eloquence");
        }));
        // 思维误导：让目标误认敌人（转移攻击目标）
        reg("error_8", new Custom("err_mislead", 10, 45, sp -> {
            List<LivingEntity> hits = ExpFx.rayTargets(sp, 10, 1);
            if (hits.isEmpty() || !(hits.get(0) instanceof Mob mob)) {
                ExpFx.noTarget(sp);
                ExpFx.refund(sp, 10);
                return;
            }
            LivingEntity newTarget = mob.level().getEntitiesOfClass(LivingEntity.class,
                            mob.getBoundingBox().inflate(10), e -> e != mob && e != sp && e.isAlive())
                    .stream().min(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(mob)))
                    .orElse(null);
            mob.setTarget(newTarget);
            ExpFx.burst(sp.serverLevel(), mob, ParticleTypes.ANGRY_VILLAGER, 8);
            ExpFx.activated(sp, "ability.guimi_mod.err_mislead");
        }));
        // 精神干扰：目标产生轻微幻觉 10 秒
        reg("error_8", new Target("err_mind_disturb", 8, 25, 10, 0, 1, 10,
                fx(MobEffects.CONFUSION, 0)));
        // 敏捷提升：移速提升
        reg("error_8", new PassiveEffect("err_agility", fx(MobEffects.MOVEMENT_SPEED, 0)));

        // ===== 错误 · 序列7 解密学者 =====
        // 线索解析：还原周围 60 秒内事件（周围生物显形 + 低语）
        reg("error_7", new Custom("err_clue_analysis", 5, 30, sp -> {
            for (LivingEntity e : ExpFx.around(sp, 10, ExpFx.Filter.ALL)) {
                ExpFx.apply(e, List.of(fx(MobEffects.GLOWING, 0)), 200, false);
            }
            ExpFx.whisper(sp);
            ExpFx.activated(sp, "ability.guimi_mod.err_clue_analysis");
        }));
        reg("error_7", new Marker("err_dream_analysis"));
        reg("error_7", new Marker("err_puzzle_deduce"));
        // 灵性直觉：感知隐藏灵体并对锁定者预警
        reg("error_7", new TickPassive("err_spirit_intuition", 60, sp -> {
            for (LivingEntity e : ExpFx.around(sp, 15, ExpFx.Filter.ALL)) {
                if (e.isInvisible() || (e instanceof Mob mob && mob.getTarget() == sp)) {
                    ExpFx.apply(e, List.of(fx(MobEffects.GLOWING, 0)), 80, true);
                }
            }
        }));
        reg("error_7", new Marker("err_trace_reading"));

        // ===== 错误 · 序列6 盗火人 =====
        // 盗火人·隔空盗窃：视线锁定 50 米内目标——潜行时把危险物品塞进对方主手并顺走其原持物，
        // 否则隔空偷取对方身上的物品：一件直接偷，多件弹出选择菜单。
        reg("error_6", new Custom("err_ability_steal", 30, 180, sp -> {
            List<LivingEntity> hits = ExpFx.rayTargets(sp, 50, 1);
            if (hits.isEmpty()) {
                ExpFx.noTarget(sp);
                ExpFx.refund(sp, 30);
                return;
            }
            LivingEntity target = hits.get(0);
            ServerLevel level = sp.serverLevel();
            if (sp.isShiftKeyDown()) {
                ItemStack dangerous = ExpFx.findDangerous(sp);
                if (dangerous.isEmpty()) {
                    dangerous = new ItemStack(Items.LAVA_BUCKET);
                }
                ItemStack held = target.getItemInHand(InteractionHand.MAIN_HAND);
                if (!held.isEmpty()) {
                    sp.getInventory().placeItemBackInInventory(held);
                }
                target.setItemInHand(InteractionHand.MAIN_HAND, dangerous);
                sp.displayClientMessage(
                        Component.translatable("message.guimi_mod.exp.steal_plant"), true);
                ExpFx.burst(level, target, ParticleTypes.SOUL, 14);
                ExpFx.activated(sp, "ability.guimi_mod.err_ability_steal");
                return;
            }
            List<ItemStack> slots = ExpFx.itemSlots(target);
            if (slots.isEmpty()) {
                ExpFx.noTarget(sp);
                ExpFx.refund(sp, 30);
                return;
            }
            if (slots.size() == 1) {
                ItemStack loot = ExpFx.stealFromIndex(target, 0);
                sp.getInventory().placeItemBackInInventory(loot);
                sp.displayClientMessage(
                        Component.translatable("message.guimi_mod.exp.steal_item",
                                loot.getHoverName()), true);
                ExpFx.burst(level, target, ParticleTypes.SOUL, 10);
                ExpFx.activated(sp, "ability.guimi_mod.err_ability_steal");
                return;
            }
            java.util.List<String> labels = new java.util.ArrayList<>();
            for (ItemStack s : slots) {
                String name = s.getHoverName().getString();
                labels.add(s.getCount() > 1 ? name + " x" + s.getCount() : name);
            }
            PacketDistributor.sendToPlayer(sp, new StealMenuPacket(target.getId(), labels));
            ExpFx.activated(sp, "ability.guimi_mod.err_ability_steal");
        }));
        // 隔空取物：将 50 米内掉落物吸到身边
        reg("error_6", new Custom("err_telekinesis", 10, 30, sp -> {
            int pulled = 0;
            for (ItemEntity item : sp.serverLevel().getEntitiesOfClass(ItemEntity.class,
                    sp.getBoundingBox().inflate(50))) {
                Vec3 pull = sp.position().subtract(item.position()).normalize().scale(1.0);
                item.setDeltaMovement(pull.x, 0.3, pull.z);
                item.hurtMarked = true;
                pulled++;
            }
            if (pulled == 0) {
                ExpFx.noTarget(sp);
                ExpFx.refund(sp, 10);
                return;
            }
            ExpFx.activated(sp, "ability.guimi_mod.err_telekinesis");
        }));
        // 宝物感知：50 米内掉落物自动高亮
        reg("error_6", new TickPassive("err_treasure_sense", 200, sp -> {
            for (ItemEntity item : sp.serverLevel().getEntitiesOfClass(ItemEntity.class,
                    sp.getBoundingBox().inflate(50))) {
                item.setGlowingTag(true);
            }
        }));
        // 体质强化：力量 / 速度 / 生命全面提升
        reg("error_6", new PassiveEffect("err_body_boost",
                fx(MobEffects.DAMAGE_BOOST, 0), fx(MobEffects.MOVEMENT_SPEED, 0),
                fx(MobEffects.HEALTH_BOOST, 1)));
        // 污染抗性：周期性缓慢清除污染（每 30 秒清 1 点）
        reg("error_6", new TickPassive("err_pollution_resist", 600,
                sp -> ExpFx.addPollution(sp, -1)));

        // ===== 门 · 序列9 学徒 =====
        // 开门：穿过面前 ≤2 格厚的墙壁
        reg("door_9", new Custom("door_open_door", 5, 10, sp -> {
            ServerLevel level = sp.serverLevel();
            Vec3 look = sp.getLookAngle();
            BlockPos front = BlockPos.containing(sp.getEyePosition().add(look));
            if (level.getBlockState(front).getCollisionShape(level, front).isEmpty()) {
                ExpFx.noTarget(sp); // 面前没有墙
                ExpFx.refund(sp, 5);
                return;
            }
            Vec3 dest = sp.position().add(look.x * 3, 0, look.z * 3);
            BlockPos dp = BlockPos.containing(dest);
            if (!level.getBlockState(dp).getCollisionShape(level, dp).isEmpty()
                    || !level.getBlockState(dp.above()).getCollisionShape(level, dp.above()).isEmpty()) {
                ExpFx.noTarget(sp); // 墙太厚，无法穿过
                ExpFx.refund(sp, 5);
                return;
            }
            ExpFx.burst(level, sp, ParticleTypes.PORTAL, 30);
            sp.teleportTo(level, dest.x, dest.y, dest.z, sp.getYRot(), sp.getXRot());
            ExpFx.activated(sp, "ability.guimi_mod.door_open_door");
        }));
        reg("door_9", new Marker("door_ritual"));
        reg("door_9", new Marker("door_space_intuition"));
        reg("door_9", new Marker("door_escape_instinct"));

        // ===== 门 · 序列8 戏法大师 =====
        // 闪光：5 米内目标致盲 3 秒
        reg("door_8", new Aoe("door_flash", 5, 12, 5, ExpFx.Filter.HOSTILE,
                false, 0, 0, false, 3, fx(MobEffects.BLINDNESS, 0), fx(MobEffects.GLOWING, 0)));
        // 黑幕：制造无光区域 8 秒
        reg("door_8", new Aoe("door_black_curtain", 5, 15, 3, ExpFx.Filter.ALL,
                false, 0, 0, false, 8, fx(MobEffects.DARKNESS, 0)));
        // 转移气体：把自身 / 友方身上的中毒转移到周围最近的敌对生物
        reg("door_8", new Custom("door_gas_transfer", 5, 10, sp -> {
            List<LivingEntity> allies = new java.util.ArrayList<>();
            allies.add(sp);
            allies.addAll(ExpFx.around(sp, 15, ExpFx.Filter.FRIENDLY));
            boolean hadPoison = false;
            for (LivingEntity e : allies) {
                if (e.hasEffect(MobEffects.POISON)) {
                    e.removeEffect(MobEffects.POISON);
                    hadPoison = true;
                    ExpFx.burst(sp.serverLevel(), e, ParticleTypes.SMOKE, 8);
                }
            }
            if (!hadPoison) {
                ExpFx.noTarget(sp);
                ExpFx.refund(sp, 5);
                return;
            }
            List<LivingEntity> hostiles = ExpFx.around(sp, 15, ExpFx.Filter.HOSTILE);
            if (!hostiles.isEmpty()) {
                LivingEntity enemy = hostiles.get(0);
                ExpFx.apply(enemy, List.of(fx(MobEffects.POISON, 0)), 200, false);
                ExpFx.burst(sp.serverLevel(), enemy, ParticleTypes.POOF, 12);
            }
            ExpFx.activated(sp, "ability.guimi_mod.door_gas_transfer");
        }));
        // 巨响：10 米内生物短暂混乱
        reg("door_8", new Aoe("door_loud_bang", 5, 15, 10, ExpFx.Filter.ALL,
                false, 0, 0, false, 5, fx(MobEffects.CONFUSION, 0)));
        // 冰冻：目标减速 5 秒
        reg("door_8", new Target("door_freeze", 6, 12, 10, 0, 1, 5,
                fx(MobEffects.MOVEMENT_SLOWDOWN, 2)));
        // 电击：接触目标麻痹 2 秒 + 伤害 3
        reg("door_8", new Target("door_shock", 5, 8, 3, 3, 1, 2,
                fx(MobEffects.MOVEMENT_SLOWDOWN, 5)));
        // 造雾：15 米范围大片灰色浓雾（烟雾粒子弥漫），范围内生物陷入黑暗 20 秒
        reg("door_8", new Custom("door_fog", 8, 25, sp -> {
            ServerLevel level = sp.serverLevel();
            for (LivingEntity e : ExpFx.around(sp, 15, ExpFx.Filter.ALL)) {
                ExpFx.apply(e, List.of(fx(MobEffects.DARKNESS, 0)), 400, false);
            }
            for (int i = 0; i < 36; i++) {
                double px = sp.getX() + (level.random.nextDouble() - 0.5) * 30;
                double py = sp.getY() + level.random.nextDouble() * 8;
                double pz = sp.getZ() + (level.random.nextDouble() - 0.5) * 30;
                ParticleOptions particle = i % 3 == 0 ? ParticleTypes.LARGE_SMOKE
                        : i % 3 == 1 ? ParticleTypes.SMOKE : ParticleTypes.ASH;
                level.sendParticles(particle, px, py, pz, 8, 0.5, 0.5, 0.5, 0.03);
            }
            level.playSound(null, sp.blockPosition(), SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.PLAYERS, 0.9F, 0.6F);
            ExpFx.activated(sp, "ability.guimi_mod.door_fog");
        }));
        // 刮风：制造大风击退周围目标
        reg("door_8", new Aoe("door_wind", 6, 10, 8, ExpFx.Filter.ALL,
                false, 0, 0, true, 0));
        // 点火：点燃 3 米内目标
        reg("door_8", new Custom("door_ignite", 3, 3, sp -> {
            List<LivingEntity> hits = ExpFx.rayTargets(sp, 3, 1);
            if (hits.isEmpty()) {
                ExpFx.noTarget(sp);
                ExpFx.refund(sp, 3);
                return;
            }
            hits.get(0).setRemainingFireTicks(100);
            ExpFx.burst(sp.serverLevel(), hits.get(0), ParticleTypes.FLAME, 10);
            ExpFx.activated(sp, "ability.guimi_mod.door_ignite");
        }));
        // 摔倒术：指定目标直接滑倒（重度减速）
        reg("door_8", new Target("door_trip", 5, 15, 10, 0, 1, 3,
                fx(MobEffects.MOVEMENT_SLOWDOWN, 4)));
        // 摔倒术·油脂区：半径 6 米地面打滑，区域内目标一并滑倒
        reg("door_8", new Aoe("door_trip_area", 8, 20, 6, ExpFx.Filter.ALL,
                false, 0, 0, false, 6, fx(MobEffects.MOVEMENT_SLOWDOWN, 3)));
        // 驱物：驱使 5 米内物品向视线方向弹跳
        reg("door_8", new Custom("door_move_object", 3, 5, sp -> {
            Vec3 look = sp.getLookAngle();
            int moved = 0;
            for (ItemEntity item : sp.serverLevel().getEntitiesOfClass(ItemEntity.class,
                    sp.getBoundingBox().inflate(5))) {
                item.setDeltaMovement(look.x * 0.8, 0.4, look.z * 0.8);
                item.hurtMarked = true;
                moved++;
            }
            if (moved == 0) {
                ExpFx.noTarget(sp);
                ExpFx.refund(sp, 3);
                return;
            }
            ExpFx.activated(sp, "ability.guimi_mod.door_move_object");
        }));
        // 逃脱戏法：瞬移 5 米
        reg("door_8", new Teleport("door_escape_trick", 8, 20, 5, false));

        // ===== 门 · 序列7 占星人 =====
        // 灵性直觉：危险预感（锁定者显形预警）
        reg("door_7", new TickPassive("door_spirit_intuition", 60, sp -> {
            for (LivingEntity e : ExpFx.around(sp, 16, ExpFx.Filter.ALL)) {
                if (e instanceof Mob mob && mob.getTarget() == sp) {
                    ExpFx.apply(e, List.of(fx(MobEffects.GLOWING, 0)), 80, true);
                }
            }
        }));
        // 占星术：水晶球占卜（获得启示与幸运）
        reg("door_7", new Custom("door_astrology", 10, 45, sp -> {
            ExpFx.whisper(sp);
            ExpFx.apply(sp, List.of(fx(MobEffects.LUCK, 0)), 1200, false);
            ExpFx.burst(sp.serverLevel(), sp, ParticleTypes.ENCHANT, 25);
            ExpFx.activated(sp, "ability.guimi_mod.door_astrology");
        }));
        reg("door_7", new Marker("door_anti_divination"));
        reg("door_7", new Marker("door_star_knowledge"));

        // ===== 门 · 序列6 记录官 =====
        // 记录：「我来到，我看见，我记录」——记录观察到的目标
        reg("door_6", new Custom("door_record", 5, 10, sp -> {
            List<LivingEntity> hits = ExpFx.rayTargets(sp, 20, 1);
            if (hits.isEmpty()) {
                ExpFx.noTarget(sp);
                ExpFx.refund(sp, 5);
                return;
            }
            sp.getPersistentData().putBoolean("gmmod_exp_recorded", true);
            sp.sendSystemMessage(Component.translatable("message.guimi_mod.exp.recorded",
                    hits.get(0).getDisplayName()));
            ExpFx.burst(sp.serverLevel(), hits.get(0), ParticleTypes.ENCHANT, 15);
        }));
        // 记录释放：消耗记录释放能力
        reg("door_6", new Custom("door_record_release", 15, 30, sp -> {
            if (!sp.getPersistentData().getBoolean("gmmod_exp_recorded")) {
                sp.displayClientMessage(
                        Component.translatable("message.guimi_mod.exp.no_record"), true);
                ExpFx.refund(sp, 15);
                return;
            }
            sp.getPersistentData().remove("gmmod_exp_recorded");
            List<LivingEntity> hits = ExpFx.rayTargets(sp, 15, 1);
            if (!hits.isEmpty()) {
                hits.get(0).hurt(sp.damageSources().indirectMagic(sp, sp), 3.0F);
                ExpFx.burst(sp.serverLevel(), hits.get(0), ParticleTypes.WITCH, 20);
            }
            ExpFx.activated(sp, "ability.guimi_mod.door_record_release");
        }));
        reg("door_6", new Marker("door_record_manage"));
        reg("door_6", new Marker("door_spirit_boost"));
    }
}
