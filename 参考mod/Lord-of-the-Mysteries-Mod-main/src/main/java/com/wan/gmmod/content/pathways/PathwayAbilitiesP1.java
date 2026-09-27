package com.wan.gmmod.content.pathways;

import com.wan.gmmod.content.spirituality.SpiritualityManager;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.common.registry.ModEntities;
import com.wan.gmmod.content.abilities.Ability;
import com.wan.gmmod.content.abilities.AbilityFx;
import com.wan.gmmod.content.abilities.AbilityRegistry;
import com.wan.gmmod.content.abilities.AbilityTemplates;
import com.wan.gmmod.content.abilities.AbilityTemplates.Aoe;
import com.wan.gmmod.content.abilities.AbilityTemplates.Custom;
import com.wan.gmmod.content.abilities.AbilityTemplates.Marker;
import com.wan.gmmod.content.abilities.AbilityTemplates.PassiveEffect;
import com.wan.gmmod.content.abilities.AbilityTemplates.SelfBuff;
import com.wan.gmmod.content.abilities.AbilityTemplates.Summon;
import com.wan.gmmod.content.abilities.AbilityTemplates.Target;
import com.wan.gmmod.content.abilities.AbilityTemplates.TickPassive;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.List;

import static com.wan.gmmod.content.abilities.AbilityFx.fx;

/**
 * 正式途径能力注册 · 第一组：倒吊人(hm_) / 空想家(vis_) / 暴君(tyr_) / 太阳(sun_)。
 * <p>
 * 已从实验体系 {@code content/exp} 中移出，作为正式途径直接生效，
 * 不再受 {@code ExperimentalPathways} 门控。
 */
final class PathwayAbilitiesP1 {

    private PathwayAbilitiesP1() {}

    private static void reg(String seq, Ability ability) {
        AbilityRegistry.register(GuimiMod.id(seq), ability);
    }

    /** 消耗背包中指定数量的物品，数量不足返回 false。 */
    static boolean consume(ServerPlayer sp, Item item, int count) {
        if (sp.getInventory().countItem(item) < count) {
            return false;
        }
        sp.getInventory().clearOrCountMatchingItems(stack -> stack.is(item),
                count, sp.inventoryMenu.getCraftSlots());
        return true;
    }

    static void init() {
        // ===== 倒吊人 · 序列9 秘祈人 =====
        // 灵感提升：10 米内存在亡灵/污染源时屏幕边缘变暗（黑暗效果近似）
        reg("hanged_man_9", new TickPassive("hm_inspiration", 60, sp -> {
            if (!AbilityFx.around(sp, 10, AbilityFx.Filter.UNDEAD).isEmpty()) {
                AbilityFx.apply(sp, List.of(fx(MobEffects.DARKNESS, 0)), 50, true);
            }
        }));
        reg("hanged_man_9", new Marker("hm_ritual_knowledge"));
        // 真实感知：默念尊名（主动触发），获得随机低语提示
        reg("hanged_man_9", new Custom("hm_true_perception", 0, 30, sp -> {
            AbilityFx.whisper(sp);
            AbilityFx.activated(sp, "ability.guimi_mod.hm_true_perception");
        }));
        // 扭曲抗性：周期性缓慢降低污染值（近似增长速率 -30%）
        reg("hanged_man_9", new TickPassive("hm_distortion_resist", 600,
                sp -> AbilityFx.addPollution(sp, -1)));

        // ===== 倒吊人 · 序列8 倾听者 =====
        // 隐秘低语：每 5 分钟一次随机低语，30% 获得临时增益，否则污染 +3
        reg("hanged_man_8", new TickPassive("hm_secret_whisper", 6000, sp -> {
            AbilityFx.whisper(sp);
            if (sp.getRandom().nextFloat() < 0.30F) {
                AbilityFx.apply(sp, List.of(fx(MobEffects.DAMAGE_BOOST, 0)), 600, false);
            } else {
                AbilityFx.addPollution(sp, 3);
            }
        }));
        // 灵性听觉：20 米内生物高亮（穿墙定位心跳）
        reg("hanged_man_8", new TickPassive("hm_spirit_hearing", 100, sp -> {
            for (LivingEntity e : AbilityFx.around(sp, 20, AbilityFx.Filter.ALL)) {
                AbilityFx.apply(e, List.of(fx(MobEffects.GLOWING, 0)), 120, true);
            }
        }));
        // 疯狂之力：消耗理智 10，随机获得力量 / 速度 / 灵性增幅 30 秒
        reg("hanged_man_8", new Custom("hm_madness_power", 0, 60, sp -> {
            AbilityFx.addSanity(sp, -10);
            switch (sp.getRandom().nextInt(3)) {
                case 0 -> AbilityFx.apply(sp, List.of(fx(MobEffects.DAMAGE_BOOST, 1)), 600, false);
                case 1 -> AbilityFx.apply(sp, List.of(fx(MobEffects.MOVEMENT_SPEED, 1)), 600, false);
                default -> AbilityFx.refund(sp, 20); // 灵性增幅
            }
            AbilityFx.activated(sp, "ability.guimi_mod.hm_madness_power");
        }));
        reg("hanged_man_8", new Marker("hm_inspiration_enhance"));

        // ===== 倒吊人 · 序列7 隐修士 =====
        // 融入阴影：需亮度 ≤3，隐身 + 提速 15 秒
        reg("hanged_man_7", new SelfBuff("hm_shadow_merge", 10, 30, 15, true, 0,
                fx(MobEffects.INVISIBILITY, 0), fx(MobEffects.MOVEMENT_SPEED, 0)));
        // 召唤阴影生物：暗影兽（攻击 6 / 生命 20），15% 概率敌对
        reg("hanged_man_7", new Summon("hm_shadow_summon", 20, 90,
                () -> ModEntities.SHADOW_CREATURE.get(), 1, 30, 20, 6, 0.15F));
        // 阴影诅咒：缓慢 + 虚弱 15 秒
        reg("hanged_man_7", new Target("hm_shadow_curse", 10, 45, 15, 0, 1, 15,
                fx(MobEffects.MOVEMENT_SLOWDOWN, 1), fx(MobEffects.WEAKNESS, 0)));
        // 操纵阴影：使 10 米内目标位移 1~3 米
        reg("hanged_man_7", new Custom("hm_shadow_manipulate", 5, 10, sp -> {
            List<LivingEntity> hits = AbilityFx.rayTargets(sp, 10, 1);
            if (hits.isEmpty()) {
                AbilityFx.noTarget(sp);
                AbilityFx.refund(sp, 5);
                return;
            }
            LivingEntity target = hits.get(0);
            double angle = sp.getRandom().nextDouble() * Math.PI * 2;
            double dist = 1 + sp.getRandom().nextDouble() * 2;
            target.setDeltaMovement(Math.cos(angle) * dist * 0.4, 0.2, Math.sin(angle) * dist * 0.4);
            target.hurtMarked = true;
            AbilityFx.burst(sp.serverLevel(), target, ParticleTypes.SQUID_INK, 15);
            AbilityFx.activated(sp, "ability.guimi_mod.hm_shadow_manipulate");
        }));
        // 阴影塑形：武器（伤害 5 + 凋零I）兼锁链（重度缓慢 5 秒）
        reg("hanged_man_7", new Target("hm_shadow_shaping", 15, 20, 12, 5, 1, 5,
                fx(MobEffects.WITHER, 0), fx(MobEffects.MOVEMENT_SLOWDOWN, 4)));

        // ===== 倒吊人 · 序列6 蔷薇主教 =====
        // 血肉魔法：生命低于 30% 时自动消耗灵性回血（每灵性 1 心）
        reg("hanged_man_6", new TickPassive("hm_flesh_magic", 20, sp -> {
            if (sp.getHealth() < sp.getMaxHealth() * 0.3F) {
                int spirit = SpiritualityManager.get(sp);
                if (spirit > 0) {
                    SpiritualityManager.set(sp, spirit - 1);
                    sp.heal(2.0F);
                    AbilityFx.burst(sp.serverLevel(), sp, ParticleTypes.DAMAGE_INDICATOR, 6);
                }
            }
        }));
        // 血肉融合：潜行加速 + 隐匿 10 秒（钻入生物体内以强化形式近似）
        reg("hanged_man_6", new SelfBuff("hm_flesh_merge", 20, 60, 10,
                fx(MobEffects.MOVEMENT_SPEED, 1), fx(MobEffects.INVISIBILITY, 0),
                fx(MobEffects.DAMAGE_RESISTANCE, 1)));
        // 血肉炸弹：撕下 1 心生命，造成 10 点伤害 + 腐蚀（中毒）
        reg("hanged_man_6", new Custom("hm_flesh_bomb", 10, 25, sp -> {
            List<LivingEntity> hits = AbilityFx.rayTargets(sp, 15, 1);
            if (hits.isEmpty()) {
                AbilityFx.noTarget(sp);
                AbilityFx.refund(sp, 10);
                return;
            }
            sp.hurt(sp.damageSources().magic(), 2.0F);
            LivingEntity target = hits.get(0);
            target.hurt(sp.damageSources().indirectMagic(sp, sp), 10.0F);
            AbilityFx.apply(target, List.of(fx(MobEffects.POISON, 1)), 60, false);
            AbilityFx.burst(sp.serverLevel(), target, ParticleTypes.DAMAGE_INDICATOR, 25);
            AbilityFx.activated(sp, "ability.guimi_mod.hm_flesh_bomb");
        }));
        // 血肉诅咒：持续伤害（凋零）10 分钟
        reg("hanged_man_6", new Target("hm_flesh_curse", 20, 120, 10, 0, 1, 600,
                fx(MobEffects.WITHER, 0)));
        // 血肉仆役：消耗 5 个腐肉 + 灵性 30，制造血肉傀儡（攻击 8 / 生命 30，2 分钟）
        reg("hanged_man_6", new Custom("hm_flesh_servant", 30, 180, sp -> {
            if (!consume(sp, Items.ROTTEN_FLESH, 5)) {
                sp.displayClientMessage(
                        Component.translatable("message.guimi_mod.exp.need_material"), true);
                AbilityFx.refund(sp, 30);
                return;
            }
            Summon.spawn(sp, EntityType.ZOMBIE, 1, 2400, 30, 8, 0F);
            AbilityFx.activated(sp, "ability.guimi_mod.hm_flesh_servant");
        }));

        // ===== 空想家 · 序列9 观众 =====
        // 情绪感知：10 米内生物情绪以粒子显示（敌对=愤怒，友善=爱心，其余=水滴）
        reg("visionary_9", new TickPassive("vis_emotion_sense", 60, sp -> {
            for (LivingEntity e : AbilityFx.around(sp, 10, AbilityFx.Filter.ALL)) {
                var particle = AbilityFx.Filter.HOSTILE.test(sp, e) ? ParticleTypes.ANGRY_VILLAGER
                        : AbilityFx.Filter.FRIENDLY.test(sp, e) ? ParticleTypes.HEART
                        : ParticleTypes.FALLING_WATER;
                AbilityFx.burst(sp.serverLevel(), e, particle, 2);
            }
        }));
        // 存在感降低：隐身 30 秒
        reg("visionary_9", new SelfBuff("vis_low_presence", 5, 45, 30,
                fx(MobEffects.INVISIBILITY, 0)));
        reg("visionary_9", new Marker("vis_micro_observe"));
        reg("visionary_9", new Marker("vis_memory_enhance"));

        // ===== 空想家 · 序列8 读心者 =====
        // 读心：读取 8 米内目标当前想法（聊天栏显示）
        reg("visionary_8", new Custom("vis_mind_read", 10, 30, sp -> {
            List<LivingEntity> hits = AbilityFx.rayTargets(sp, 8, 1);
            if (hits.isEmpty()) {
                AbilityFx.noTarget(sp);
                AbilityFx.refund(sp, 10);
                return;
            }
            LivingEntity target = hits.get(0);
            boolean aggro = target instanceof Mob mob && mob.getTarget() != null;
            sp.sendSystemMessage(Component.translatable(
                    aggro ? "message.guimi_mod.exp.thought.hostile" : "message.guimi_mod.exp.thought.calm",
                    target.getDisplayName()));
        }));
        // 情绪操控：使目标转而攻击最近的其他生物
        reg("visionary_8", new Custom("vis_emotion_control", 15, 45, sp -> {
            List<LivingEntity> hits = AbilityFx.rayTargets(sp, 8, 1);
            if (hits.isEmpty() || !(hits.get(0) instanceof Mob mob)) {
                AbilityFx.noTarget(sp);
                AbilityFx.refund(sp, 15);
                return;
            }
            LivingEntity newTarget = mob.level().getEntitiesOfClass(LivingEntity.class,
                            mob.getBoundingBox().inflate(10), e -> e != mob && e != sp && e.isAlive())
                    .stream().min(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(mob)))
                    .orElse(null);
            mob.setTarget(newTarget);
            AbilityFx.apply(mob, List.of(fx(MobEffects.CONFUSION, 0)), 300, false);
            AbilityFx.burst(sp.serverLevel(), mob, ParticleTypes.ANGRY_VILLAGER, 8);
            AbilityFx.activated(sp, "ability.guimi_mod.vis_emotion_control");
        }));
        reg("visionary_8", new Marker("vis_lie_detect"));
        reg("visionary_8", new Marker("vis_mind_barrier"));

        // ===== 空想家 · 序列7 心理医生 =====
        // 心理治疗：移除目标负面效果并恢复 20 点理智（无目标时作用于自身）
        reg("visionary_7", new Custom("vis_therapy", 15, 60, sp -> {
            List<LivingEntity> hits = AbilityFx.rayTargets(sp, 8, 1);
            LivingEntity target = hits.isEmpty() ? sp : hits.get(0);
            AbilityFx.clearNegative(target);
            if (target instanceof ServerPlayer op) {
                AbilityFx.addSanity(op, 20);
            }
            AbilityFx.burst(sp.serverLevel(), target, ParticleTypes.HEART, 10);
            AbilityFx.activated(sp, "ability.guimi_mod.vis_therapy");
        }));
        // 催眠暗示：目标进入暗示状态（迟缓恍惚）60 秒
        reg("visionary_7", new Target("vis_hypnotic_hint", 15, 120, 5, 0, 1, 60,
                fx(MobEffects.MOVEMENT_SLOWDOWN, 2), fx(MobEffects.WEAKNESS, 1)));
        reg("visionary_7", new Marker("vis_psycho_analysis"));
        // 安抚：8 米内所有生物敌意下降（清除仇恨并虚弱）
        reg("visionary_7", new Custom("vis_pacify", 15, 90, sp -> {
            for (LivingEntity e : AbilityFx.around(sp, 8, AbilityFx.Filter.ALL)) {
                if (e instanceof Mob mob) {
                    mob.setTarget(null);
                }
                AbilityFx.apply(e, List.of(fx(MobEffects.WEAKNESS, 0)), 200, false);
            }
            AbilityFx.burst(sp.serverLevel(), sp, ParticleTypes.HEART, 16);
            AbilityFx.activated(sp, "ability.guimi_mod.vis_pacify");
        }));

        // ===== 空想家 · 序列6 催眠师 =====
        // 深层催眠：植入后门指令（长时间恍惚 30 分钟）
        reg("visionary_6", new Target("vis_deep_hypnosis", 25, 180, 8, 0, 1, 1800,
                fx(MobEffects.MOVEMENT_SLOWDOWN, 1), fx(MobEffects.WEAKNESS, 1)));
        // 群体暗示：10 米内所有生物接受简单指令（原地停滞 30 秒）
        reg("visionary_6", new Aoe("vis_group_hint", 20, 120, 10, AbilityFx.Filter.ALL,
                false, 0, 0, false, 30, fx(MobEffects.MOVEMENT_SLOWDOWN, 3)));
        // 梦境编织：幻象区域（反胃 + 黑暗）20 秒
        reg("visionary_6", new Aoe("vis_dream_weave", 20, 60, 6, AbilityFx.Filter.ALL,
                false, 0, 0, false, 20, fx(MobEffects.CONFUSION, 0), fx(MobEffects.DARKNESS, 0)));
        // 记忆编辑：抹除目标当前仇恨记忆
        reg("visionary_6", new Custom("vis_memory_edit", 20, 180, sp -> {
            List<LivingEntity> hits = AbilityFx.rayTargets(sp, 8, 1);
            if (hits.isEmpty() || !(hits.get(0) instanceof Mob mob)) {
                AbilityFx.noTarget(sp);
                AbilityFx.refund(sp, 20);
                return;
            }
            mob.setTarget(null);
            mob.setLastHurtByMob(null);
            AbilityFx.burst(sp.serverLevel(), mob, ParticleTypes.ENCHANT, 20);
            AbilityFx.activated(sp, "ability.guimi_mod.vis_memory_edit");
        }));
        reg("visionary_6", new Marker("vis_mind_dominate"));

        // ===== 暴君 · 序列9 水手 =====
        // 游泳精通：海豚恩惠 + 水下呼吸
        reg("tyrant_9", new PassiveEffect("tyr_swim_mastery",
                fx(MobEffects.DOLPHINS_GRACE, 0), fx(MobEffects.WATER_BREATHING, 0)));
        // 力量强化：常驻力量I，水中力量II
        reg("tyrant_9", new TickPassive("tyr_strength_boost", 40, sp ->
                AbilityFx.apply(sp, List.of(fx(MobEffects.DAMAGE_BOOST, sp.isInWater() ? 1 : 0)), 100, true)));
        // 幻鳞：由事件实现 20% 概率伤害减半
        reg("tyrant_9", new Marker("tyr_phantom_scales"));
        reg("tyrant_9", new Marker("tyr_balance"));

        // ===== 暴君 · 序列8 暴怒之民 =====
        // 暴怒：受伤叠层，由事件实现
        reg("tyrant_8", new Marker("tyr_rage"));
        // 暴怒一击：消耗全部愤怒层数释放重击
        reg("tyrant_8", new Custom("tyr_rage_strike", 5, 15, sp -> {
            List<LivingEntity> hits = AbilityFx.rayTargets(sp, 4, 1);
            if (hits.isEmpty()) {
                AbilityFx.noTarget(sp);
                AbilityFx.refund(sp, 5);
                return;
            }
            int stacks = sp.getPersistentData().getInt("gmmod_exp_rage");
            sp.getPersistentData().putInt("gmmod_exp_rage", 0);
            LivingEntity target = hits.get(0);
            target.hurt(sp.damageSources().playerAttack(sp), 6.0F * (1.0F + stacks * 0.2F));
            target.knockback(1.2, sp.getX() - target.getX(), sp.getZ() - target.getZ());
            AbilityFx.apply(target, List.of(fx(MobEffects.MOVEMENT_SLOWDOWN, 3),
                    fx(MobEffects.CONFUSION, 0)), 60, false);
            AbilityFx.burst(sp.serverLevel(), target, ParticleTypes.CRIT, 25);
            AbilityFx.activated(sp, "ability.guimi_mod.tyr_rage_strike");
        }));
        // 体质强化：最大生命提升 + 自然恢复
        reg("tyrant_8", new PassiveEffect("tyr_constitution",
                fx(MobEffects.HEALTH_BOOST, 1), fx(MobEffects.REGENERATION, 0)));
        reg("tyrant_8", new Marker("tyr_storm_resist"));

        // ===== 暴君 · 序列7 航海家 =====
        reg("tyrant_7", new Marker("tyr_navigation"));
        // 海洋亲和：水中全属性提升
        reg("tyrant_7", new TickPassive("tyr_ocean_affinity", 40, sp -> {
            if (sp.isInWater()) {
                AbilityFx.apply(sp, List.of(fx(MobEffects.DAMAGE_BOOST, 0),
                        fx(MobEffects.MOVEMENT_SPEED, 0), fx(MobEffects.DAMAGE_RESISTANCE, 0)), 100, true);
            }
        }));
        // 水流操控：水弹（伤害 4 + 击退），并为周围友方附加水下呼吸
        reg("tyrant_7", new Custom("tyr_water_control", 8, 8, sp -> {
            for (LivingEntity ally : AbilityFx.around(sp, 5, AbilityFx.Filter.FRIENDLY)) {
                AbilityFx.apply(ally, List.of(fx(MobEffects.WATER_BREATHING, 0)), 600, false);
            }
            List<LivingEntity> hits = AbilityFx.rayTargets(sp, 15, 1);
            if (!hits.isEmpty()) {
                LivingEntity target = hits.get(0);
                target.hurt(sp.damageSources().indirectMagic(sp, sp), 4.0F);
                target.knockback(1.0, sp.getX() - target.getX(), sp.getZ() - target.getZ());
                AbilityFx.burst(sp.serverLevel(), target, ParticleTypes.SPLASH, 30);
            }
            AbilityFx.activated(sp, "ability.guimi_mod.tyr_water_control");
        }));

        // ===== 暴君 · 序列6 风眷者 =====
        // 御风：自身 + 8 米内友方移速提升 20 秒
        reg("tyrant_6", new Aoe("tyr_wind_blessing", 15, 30, 8, AbilityFx.Filter.FRIENDLY,
                true, 0, 0, false, 20, fx(MobEffects.MOVEMENT_SPEED, 1)));
        // 风刃：伤害 5，射程 20，穿透 2 个目标
        reg("tyrant_6", new Target("tyr_wind_blade", 8, 4, 20, 5, 2, 0));
        // 风之翼：缓降 + 跳跃提升 30 秒
        reg("tyrant_6", new SelfBuff("tyr_wind_wings", 10, 60, 30,
                fx(MobEffects.SLOW_FALLING, 0), fx(MobEffects.JUMP, 2)));
        // 飓风护体：击退 3 米内敌人并获得抗性 10 秒
        reg("tyrant_6", new Custom("tyr_hurricane_guard", 15, 30, sp -> {
            for (LivingEntity e : AbilityFx.around(sp, 3, AbilityFx.Filter.HOSTILE)) {
                e.knockback(1.5, sp.getX() - e.getX(), sp.getZ() - e.getZ());
            }
            AbilityFx.apply(sp, List.of(fx(MobEffects.DAMAGE_RESISTANCE, 0)), 200, false);
            AbilityFx.burst(sp.serverLevel(), sp, ParticleTypes.CLOUD, 40);
            AbilityFx.activated(sp, "ability.guimi_mod.tyr_hurricane_guard");
        }));

        // ===== 太阳 · 序列9 歌颂者 =====
        // 勇气之歌：8 米内友方抗性I + 免疫恐惧 20 秒
        reg("sun_9", new Aoe("sun_courage_song", 10, 30, 8, AbilityFx.Filter.FRIENDLY,
                true, 0, 0, false, 20, fx(MobEffects.DAMAGE_RESISTANCE, 0)));
        // 净化之音：对 6 米内亡灵造成 4 点伤害 + 击退
        reg("sun_9", new Aoe("sun_purify_sound", 8, 10, 6, AbilityFx.Filter.UNDEAD,
                false, 4, 0, true, 0));
        // 体质强化：最大生命 +4
        reg("sun_9", new PassiveEffect("sun_constitution", fx(MobEffects.HEALTH_BOOST, 0)));
        // 虔诚光环：2 米内友方灵性恢复加快
        reg("sun_9", new TickPassive("sun_pious_aura", 100, sp -> {
            for (LivingEntity e : AbilityFx.around(sp, 2, AbilityFx.Filter.FRIENDLY)) {
                if (e instanceof ServerPlayer op) {
                    AbilityFx.refund(op, 1); // 补充少量灵性
                }
            }
        }));

        // ===== 太阳 · 序列8 祈光人 =====
        // 白昼：照亮周围（自身发光 + 夜视）60 秒
        reg("sun_8", new SelfBuff("sun_daylight", 5, 10, 60,
                fx(MobEffects.NIGHT_VISION, 0), fx(MobEffects.GLOWING, 0)));
        // 祝福：目标获得抗恐惧 / 火焰 + 对亡灵增伤 30 秒（无目标时作用于自身）
        reg("sun_8", new Custom("sun_blessing", 10, 20, sp -> {
            List<LivingEntity> hits = AbilityFx.rayTargets(sp, 10, 1);
            LivingEntity target = hits.isEmpty() ? sp : hits.get(0);
            AbilityFx.apply(target, List.of(fx(MobEffects.FIRE_RESISTANCE, 0),
                    fx(MobEffects.DAMAGE_BOOST, 0)), 600, false);
            AbilityFx.burst(sp.serverLevel(), target, ParticleTypes.END_ROD, 15);
            AbilityFx.activated(sp, "ability.guimi_mod.sun_blessing");
        }));
        // 日照：8 米内亡灵伤害 8 + 致盲 5 秒
        reg("sun_8", new Aoe("sun_sunshine", 15, 30, 8, AbilityFx.Filter.UNDEAD,
                false, 8, 0, false, 5, fx(MobEffects.BLINDNESS, 0), fx(MobEffects.GLOWING, 0)));
        reg("sun_8", new Marker("sun_holy_water"));

        // ===== 太阳 · 序列7 太阳神官 =====
        // 召唤圣光：3 米内亡灵伤害 12，其他敌对生物伤害 6
        reg("sun_7", new Custom("sun_holy_light", 15, 18, sp -> {
            for (LivingEntity e : AbilityFx.around(sp, 3, AbilityFx.Filter.HOSTILE)) {
                e.hurt(sp.damageSources().indirectMagic(sp, sp),
                        e.isInvertedHealAndHarm() ? 12.0F : 6.0F);
                AbilityFx.burst(sp.serverLevel(), e, ParticleTypes.END_ROD, 20);
            }
            AbilityFx.burst(sp.serverLevel(), sp, ParticleTypes.END_ROD, 40);
            AbilityFx.activated(sp, "ability.guimi_mod.sun_holy_light");
        }));
        // 光明之火：6 米内亡灵伤害 8 + 点燃 10 秒
        reg("sun_7", new Custom("sun_light_fire", 15, 25, sp -> {
            for (LivingEntity e : AbilityFx.around(sp, 6, AbilityFx.Filter.UNDEAD)) {
                e.hurt(sp.damageSources().indirectMagic(sp, sp), 8.0F);
                e.setRemainingFireTicks(200);
                AbilityFx.burst(sp.serverLevel(), e, ParticleTypes.FLAME, 15);
            }
            AbilityFx.activated(sp, "ability.guimi_mod.sun_light_fire");
        }));
        // 净化之斩：武器附加神圣伤害 20 秒
        reg("sun_7", new SelfBuff("sun_purify_slash", 10, 15, 20, fx(MobEffects.DAMAGE_BOOST, 1)));
        // 免疫恐惧：为自身与 15 米内友方清除黑暗 / 失明
        reg("sun_7", new TickPassive("sun_fear_immunity", 60, sp -> {
            sp.removeEffect(MobEffects.DARKNESS);
            sp.removeEffect(MobEffects.BLINDNESS);
            for (LivingEntity e : AbilityFx.around(sp, 15, AbilityFx.Filter.FRIENDLY)) {
                e.removeEffect(MobEffects.DARKNESS);
                e.removeEffect(MobEffects.BLINDNESS);
            }
        }));
        // 太阳光环：20 米内友方勇气 + 力量 + 敏捷提升 30 秒
        reg("sun_7", new Aoe("sun_solar_aura", 20, 60, 20, AbilityFx.Filter.FRIENDLY,
                true, 0, 0, false, 30, fx(MobEffects.DAMAGE_BOOST, 0), fx(MobEffects.MOVEMENT_SPEED, 0)));
        // 神圣誓约：为武器附加火焰 / 神圣属性 30 秒
        reg("sun_7", new SelfBuff("sun_holy_oath", 5, 5, 30,
                fx(MobEffects.DAMAGE_BOOST, 0), fx(MobEffects.FIRE_RESISTANCE, 0)));

        // ===== 太阳 · 序列6 公证人 =====
        // 体质强化：最大生命 +8，伤害 +15%
        reg("sun_6", new PassiveEffect("sun_notary_constitution",
                fx(MobEffects.HEALTH_BOOST, 1), fx(MobEffects.DAMAGE_BOOST, 0)));
    }
}