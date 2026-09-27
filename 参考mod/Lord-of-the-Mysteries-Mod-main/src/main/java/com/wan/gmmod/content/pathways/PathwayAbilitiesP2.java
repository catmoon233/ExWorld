package com.wan.gmmod.content.pathways;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.abilities.Ability;
import com.wan.gmmod.content.abilities.AbilityFx;
import com.wan.gmmod.content.abilities.AbilityRegistry;
import com.wan.gmmod.content.abilities.AbilityTemplates.Aoe;
import com.wan.gmmod.content.abilities.AbilityTemplates.Aura;
import com.wan.gmmod.content.abilities.AbilityTemplates.Custom;
import com.wan.gmmod.content.abilities.AbilityTemplates.Dash;
import com.wan.gmmod.content.abilities.AbilityTemplates.Marker;
import com.wan.gmmod.content.abilities.AbilityTemplates.PassiveEffect;
import com.wan.gmmod.content.abilities.AbilityTemplates.SelfBuff;
import com.wan.gmmod.content.abilities.AbilityTemplates.Summon;
import com.wan.gmmod.content.abilities.AbilityTemplates.Target;
import com.wan.gmmod.content.abilities.AbilityTemplates.Teleport;
import com.wan.gmmod.content.abilities.AbilityTemplates.TickPassive;
import com.wan.gmmod.content.abilities.LightStormAbility;
import com.wan.gmmod.common.registry.ModEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.List;

import static com.wan.gmmod.content.abilities.AbilityFx.fx;

/**
 * 正式途径能力注册 · 第二组：白塔(wt_) / 黄昏巨人(gia_) / 黑暗(dark_) / 死神(dea_)。
 * <p>
 * 已从实验体系 {@code content/exp} 中移出，作为正式途径直接生效，
 * 不再受 {@code ExperimentalPathways} 门控。
 */
final class PathwayAbilitiesP2 {

    private PathwayAbilitiesP2() {}

    private static void reg(String seq, Ability ability) {
        AbilityRegistry.register(GuimiMod.id(seq), ability);
    }

    /** 向玩家发送目标的「解析」信息（名称 + 生命）。 */
    private static void analyze(ServerPlayer sp, LivingEntity target) {
        sp.sendSystemMessage(Component.translatable("message.guimi_mod.exp.analyze",
                target.getDisplayName(),
                String.format("%.0f", target.getHealth()),
                String.format("%.0f", target.getMaxHealth())));
    }

    static void init() {
        // ===== 白塔 · 序列9 阅读者 =====
        reg("white_tower_9", new Marker("wt_speed_reading"));
        reg("white_tower_9", new Marker("wt_knowledge_store"));
        reg("white_tower_9", new Marker("wt_ritual_magic"));

        // ===== 白塔 · 序列8 推理学员 =====
        // 线索拼接：标记目标使其高亮，读取交互痕迹
        reg("white_tower_8", new Custom("wt_clue_piece", 5, 20, sp -> {
            List<LivingEntity> hits = AbilityFx.rayTargets(sp, 10, 1);
            if (hits.isEmpty()) {
                AbilityFx.noTarget(sp);
                AbilityFx.refund(sp, 5);
                return;
            }
            AbilityFx.apply(hits.get(0), List.of(fx(MobEffects.GLOWING, 0)), 200, false);
            analyze(sp, hits.get(0));
        }));
        reg("white_tower_8", new Marker("wt_observation"));
        reg("white_tower_8", new Marker("wt_ritual_mastery"));

        // ===== 白塔 · 序列7 守知者 =====
        // 案件重演：以粒子幻影重放 10 米内事件，并标记周围生物
        reg("white_tower_7", new Custom("wt_case_replay", 10, 60, sp -> {
            for (LivingEntity e : AbilityFx.around(sp, 10, AbilityFx.Filter.ALL)) {
                AbilityFx.apply(e, List.of(fx(MobEffects.GLOWING, 0)), 300, false);
                AbilityFx.burst(sp.serverLevel(), e, ParticleTypes.ENCHANT, 15);
            }
            AbilityFx.burst(sp.serverLevel(), sp, ParticleTypes.ENCHANT, 40);
            AbilityFx.activated(sp, "ability.guimi_mod.wt_case_replay");
        }));
        // 格斗精通：近战伤害提升
        reg("white_tower_7", new PassiveEffect("wt_combat_mastery", fx(MobEffects.DAMAGE_BOOST, 0)));
        // 弱点锁定：分析目标后 30 秒内对其增伤（自身力量 + 目标发光）
        reg("white_tower_7", new Custom("wt_weakness_lock", 10, 45, sp -> {
            List<LivingEntity> hits = AbilityFx.rayTargets(sp, 15, 1);
            if (hits.isEmpty()) {
                AbilityFx.noTarget(sp);
                AbilityFx.refund(sp, 10);
                return;
            }
            AbilityFx.apply(hits.get(0), List.of(fx(MobEffects.GLOWING, 0)), 600, false);
            AbilityFx.apply(sp, List.of(fx(MobEffects.DAMAGE_BOOST, 0)), 600, false);
            AbilityFx.activated(sp, "ability.guimi_mod.wt_weakness_lock");
        }));

        // ===== 白塔 · 序列6 博学者 =====
        // 解析：辨识目标详情，并记录以供「模仿」
        reg("white_tower_6", new Custom("wt_analyze", 5, 15, sp -> {
            List<LivingEntity> hits = AbilityFx.rayTargets(sp, 20, 1);
            if (hits.isEmpty()) {
                AbilityFx.noTarget(sp);
                AbilityFx.refund(sp, 5);
                return;
            }
            analyze(sp, hits.get(0));
            sp.getPersistentData().putBoolean("gmmod_exp_analyzed", true);
        }));
        // 模仿：复制解析过的能力释放一次（威力为原版的一部分）
        reg("white_tower_6", new Custom("wt_mimic", 40, 90, sp -> {
            if (!sp.getPersistentData().getBoolean("gmmod_exp_analyzed")) {
                sp.displayClientMessage(
                        Component.translatable("message.guimi_mod.exp.no_record"), true);
                AbilityFx.refund(sp, 40);
                return;
            }
            sp.getPersistentData().remove("gmmod_exp_analyzed");
            List<LivingEntity> hits = AbilityFx.rayTargets(sp, 15, 1);
            if (!hits.isEmpty()) {
                hits.get(0).hurt(sp.damageSources().indirectMagic(sp, sp), 4.0F);
                AbilityFx.burst(sp.serverLevel(), hits.get(0), ParticleTypes.WITCH, 20);
            }
            AbilityFx.activated(sp, "ability.guimi_mod.wt_mimic");
        }));
        // 学者光环：10 米内友方获得幸运（经验加成近似）
        reg("white_tower_6", new Aura("wt_scholar_aura", 10, AbilityFx.Filter.FRIENDLY, true,
                fx(MobEffects.LUCK, 0)));

        // ===== 黄昏巨人 · 序列9 战士 =====
        // 战斗本能：近战伤害 + 攻击速度提升
        reg("giant_9", new PassiveEffect("gia_battle_instinct",
                fx(MobEffects.DAMAGE_BOOST, 0), fx(MobEffects.DIG_SPEED, 0)));
        reg("giant_9", new Marker("gia_weapon_mastery"));
        reg("giant_9", new Marker("gia_fist_mastery"));
        // 坚韧：最大生命提升 + 自然恢复
        reg("giant_9", new PassiveEffect("gia_toughness",
                fx(MobEffects.HEALTH_BOOST, 1), fx(MobEffects.REGENERATION, 0)));

        // ===== 黄昏巨人 · 序列8 格斗家 =====
        // 连击 / 格挡反击 / 超凡抗性：由事件实现
        reg("giant_8", new Marker("gia_combo"));
        reg("giant_8", new Marker("gia_perfect_block"));
        reg("giant_8", new Marker("gia_transcendent_resist"));
        // 摔投：将 3 米内目标摔到身后 5 米并造成落地伤害
        reg("giant_8", new Custom("gia_throw", 5, 10, sp -> {
            List<LivingEntity> hits = AbilityFx.rayTargets(sp, 3, 1);
            if (hits.isEmpty()) {
                AbilityFx.noTarget(sp);
                AbilityFx.refund(sp, 5);
                return;
            }
            LivingEntity target = hits.get(0);
            var behind = sp.position().subtract(sp.getLookAngle().scale(5));
            target.teleportTo(behind.x, sp.getY() + 0.5, behind.z);
            target.hurt(sp.damageSources().playerAttack(sp), 4.0F);
            AbilityFx.burst(sp.serverLevel(), target, ParticleTypes.CRIT, 20);
            AbilityFx.activated(sp, "ability.guimi_mod.gia_throw");
        }));

        // ===== 黄昏巨人 · 序列7 武器大师 =====
        // 武器宗师：任何武器伤害 + 攻速大幅提升
        reg("giant_7", new PassiveEffect("gia_weapon_grandmaster",
                fx(MobEffects.DAMAGE_BOOST, 1), fx(MobEffects.DIG_SPEED, 1)));
        // 旋风斩：360° 横扫 3 米内所有目标
        reg("giant_7", new Aoe("gia_whirlwind", 10, 15, 3, AbilityFx.Filter.HOSTILE,
                false, 6, 0, true, 0));
        // 武器附魔：消耗灵性临时强化武器 10 秒
        reg("giant_7", new SelfBuff("gia_weapon_enchant", 10, 10, 10, fx(MobEffects.DAMAGE_BOOST, 1)));

        // ===== 黄昏巨人 · 序列6 黎明骑士 =====
        // 黎明光环：8 米内友方力量I + 抗性I，亡灵虚弱II
        reg("giant_6", new TickPassive("gia_dawn_aura", 40, sp -> {
            List<AbilityFx.Effect> ally = List.of(fx(MobEffects.DAMAGE_BOOST, 0),
                    fx(MobEffects.DAMAGE_RESISTANCE, 0));
            AbilityFx.apply(sp, ally, 100, true);
            for (LivingEntity e : AbilityFx.around(sp, 8, AbilityFx.Filter.FRIENDLY)) {
                AbilityFx.apply(e, ally, 100, true);
            }
            for (LivingEntity e : AbilityFx.around(sp, 8, AbilityFx.Filter.UNDEAD)) {
                AbilityFx.apply(e, List.of(fx(MobEffects.WEAKNESS, 1)), 100, true);
            }
        }));
        // 黎明冲锋：冲锋 10 米，路径伤害 7 + 击飞
        reg("giant_6", new Dash("gia_dawn_charge", 10, 18, 10, 7));
        // 黎明斩：强力斩击 ×2.5，亡灵点燃 10 秒
        reg("giant_6", new Custom("gia_dawn_slash", 10, 25, sp -> {
            List<LivingEntity> hits = AbilityFx.rayTargets(sp, 4, 1);
            if (hits.isEmpty()) {
                AbilityFx.noTarget(sp);
                AbilityFx.refund(sp, 10);
                return;
            }
            LivingEntity target = hits.get(0);
            target.hurt(sp.damageSources().playerAttack(sp), 12.0F);
            if (target.isInvertedHealAndHarm()) {
                target.setRemainingFireTicks(200);
            }
            AbilityFx.burst(sp.serverLevel(), target, ParticleTypes.SWEEP_ATTACK, 4);
            AbilityFx.activated(sp, "ability.guimi_mod.gia_dawn_slash");
        }));
        // 黎明铠甲：光之铠甲吸收约 15 点伤害，30 秒；生效期间在玩家身上渲染黎明铠甲模型
        reg("giant_6", new Custom("gia_dawn_armor", 15, 60, sp -> {
            sp.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 3, false, true, true));
            sp.addEffect(new MobEffectInstance(ModEffects.DAWN_ARMOR_ACTIVE, 600, 0, false, false, false));
            AbilityFx.burst(sp.serverLevel(), sp, ParticleTypes.ENCHANT, 20);
            AbilityFx.activated(sp, "ability.guimi_mod.gia_dawn_armor");
        }));
        // 晨曦之剑：凝聚光之巨剑（大幅增伤 45 秒）；期间在玩家手中渲染晨曦之剑
        reg("giant_6", new Custom("gia_dawn_sword", 20, 90, sp -> {
            sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 900, 2, false, true, true));
            sp.addEffect(new MobEffectInstance(ModEffects.DAWN_SWORD_ACTIVE, 900, 0, false, false, false));
            AbilityFx.burst(sp.serverLevel(), sp, ParticleTypes.END_ROD, 24);
            AbilityFx.activated(sp, "ability.guimi_mod.gia_dawn_sword");
        }));
        // 光之风暴：刺入晨曦之剑并崩解，化作毁灭性光刃飓风（范围持续伤害，亡灵受圣光灼烧）
        reg("giant_6", new LightStormAbility(30, 6, 6, 5, 40, 5, 60));

        // ===== 黑暗 · 序列9 不眠者 =====
        // 夜行：夜间移速 + 视野提升
        reg("darkness_9", new TickPassive("dark_night_walk", 40, sp -> {
            if (!sp.serverLevel().isDay()) {
                AbilityFx.apply(sp, List.of(fx(MobEffects.MOVEMENT_SPEED, 0),
                        fx(MobEffects.NIGHT_VISION, 0)), 100, true);
            }
        }));
        reg("darkness_9", new Marker("dark_sleep_resist"));
        // 黑暗感知：黑暗中轮廓可见（常驻夜视）
        reg("darkness_9", new PassiveEffect("dark_dark_sense", fx(MobEffects.NIGHT_VISION, 0)));
        reg("darkness_9", new Marker("dark_mind_tough"));

        // ===== 黑暗 · 序列8 午夜诗人 =====
        // 暗影诗篇：自身 + 8 米内友方夜视 + 隐身 20 秒
        reg("darkness_8", new Aoe("dark_shadow_poem", 10, 60, 8, AbilityFx.Filter.FRIENDLY,
                true, 0, 0, false, 20, fx(MobEffects.NIGHT_VISION, 0), fx(MobEffects.INVISIBILITY, 0)));
        // 恐惧低语：恐惧（黑暗+缓慢）+ 挖掘疲劳II，10 秒
        reg("darkness_8", new Target("dark_fear_whisper", 10, 25, 12, 0, 1, 10,
                fx(MobEffects.DARKNESS, 0), fx(MobEffects.DIG_SLOWDOWN, 1),
                fx(MobEffects.MOVEMENT_SLOWDOWN, 0)));
        // 暗影步：瞬移至 10 米内亮度 ≤3 的位置
        reg("darkness_8", new Teleport("dark_shadow_step", 10, 20, 10, true));
        // 诗篇·镇静：消除队友（或自身）的恐惧 / 混乱
        reg("darkness_8", new Custom("dark_calm_poem", 5, 15, sp -> {
            List<LivingEntity> hits = AbilityFx.rayTargets(sp, 10, 1);
            LivingEntity target = hits.isEmpty() ? sp : hits.get(0);
            AbilityFx.clearNegative(target);
            AbilityFx.burst(sp.serverLevel(), target, ParticleTypes.NOTE, 12);
            AbilityFx.activated(sp, "ability.guimi_mod.dark_calm_poem");
        }));

        // ===== 黑暗 · 序列7 梦魇 =====
        // 入梦：将 15 米内目标拖入梦境（沉睡 15 秒）
        reg("darkness_7", new Target("dark_dream_pull", 15, 45, 15, 0, 1, 15,
                fx(MobEffects.MOVEMENT_SLOWDOWN, 5), fx(MobEffects.BLINDNESS, 0)));
        // 塑梦：控制梦境施加恐惧与伤害
        reg("darkness_7", new Target("dark_dream_shape", 15, 60, 15, 3, 1, 10,
                fx(MobEffects.DARKNESS, 0), fx(MobEffects.CONFUSION, 0)));
        // 噩梦侵袭：持续虚灵伤害（凋零），期间难以回血
        reg("darkness_7", new Target("dark_nightmare", 15, 30, 15, 2, 1, 10,
                fx(MobEffects.WITHER, 0)));
        reg("darkness_7", new Marker("dark_dream_travel"));

        // ===== 黑暗 · 序列6 安魂师 =====
        // 安魂：15 米内亡灵中立化 30 秒
        reg("darkness_6", new Custom("dark_requiem", 15, 60, sp -> {
            for (LivingEntity e : AbilityFx.around(sp, 15, AbilityFx.Filter.UNDEAD)) {
                if (e instanceof Mob mob) {
                    mob.setTarget(null);
                }
                AbilityFx.apply(e, List.of(fx(MobEffects.WEAKNESS, 1)), 600, false);
                AbilityFx.burst(sp.serverLevel(), e, ParticleTypes.SOUL, 8);
            }
            AbilityFx.activated(sp, "ability.guimi_mod.dark_requiem");
        }));
        // 灵体视觉：使隐身生物显形
        reg("darkness_6", new TickPassive("dark_spirit_vision", 60, sp -> {
            for (LivingEntity e : AbilityFx.around(sp, 20, AbilityFx.Filter.ALL)) {
                if (e.isInvisible()) {
                    AbilityFx.apply(e, List.of(fx(MobEffects.GLOWING, 0)), 80, true);
                }
            }
        }));
        // 灵魂治疗：移除疯狂 / 恐惧 / 混乱，恢复理智 20 点
        reg("darkness_6", new Custom("dark_soul_heal", 15, 45, sp -> {
            List<LivingEntity> hits = AbilityFx.rayTargets(sp, 10, 1);
            LivingEntity target = hits.isEmpty() ? sp : hits.get(0);
            AbilityFx.clearNegative(target);
            if (target instanceof ServerPlayer op) {
                AbilityFx.addSanity(op, 20);
            }
            AbilityFx.burst(sp.serverLevel(), target, ParticleTypes.SOUL_FIRE_FLAME, 12);
            AbilityFx.activated(sp, "ability.guimi_mod.dark_soul_heal");
        }));
        reg("darkness_6", new Marker("dark_dream_range"));
        reg("darkness_6", new Marker("dark_requiem_song"));

        // ===== 死神 · 序列9 收尸人 =====
        // 亡灵感知：15 米内亡灵高亮
        reg("death_9", new TickPassive("dea_undead_sense", 100, sp -> {
            for (LivingEntity e : AbilityFx.around(sp, 15, AbilityFx.Filter.UNDEAD)) {
                AbilityFx.apply(e, List.of(fx(MobEffects.GLOWING, 0)), 120, true);
            }
        }));
        reg("death_9", new Marker("dea_corpse_preserve"));
        reg("death_9", new Marker("dea_death_resist"));
        reg("death_9", new Marker("dea_spirit_sight"));
        // 阳光弱点：白昼直射阳光下获得虚弱（途径代价）
        reg("death_9", new TickPassive("dea_sun_weakness", 100, sp -> {
            if (sp.serverLevel().isDay() && sp.serverLevel().canSeeSky(sp.blockPosition())) {
                AbilityFx.apply(sp, List.of(fx(MobEffects.WEAKNESS, 0)), 120, true);
            }
        }));

        // ===== 死神 · 序列8 掘墓人 =====
        // 死亡之眼：灵性视角观察，短时间伤害提升
        reg("death_8", new SelfBuff("dea_death_eye", 5, 20, 30, fx(MobEffects.DAMAGE_BOOST, 0)));
        reg("death_8", new Marker("dea_grave_intuition"));
        // 体魄强化：生命 + 力量提升
        reg("death_8", new PassiveEffect("dea_body_boost",
                fx(MobEffects.HEALTH_BOOST, 0), fx(MobEffects.DAMAGE_BOOST, 0)));

        // ===== 死神 · 序列7 通灵者 =====
        // 通灵：与目标之灵直接沟通获取真实信息
        reg("death_7", new Custom("dea_commune", 10, 30, sp -> {
            List<LivingEntity> hits = AbilityFx.rayTargets(sp, 15, 1);
            if (hits.isEmpty()) {
                AbilityFx.whisper(sp);
            } else {
                analyze(sp, hits.get(0));
            }
            AbilityFx.burst(sp.serverLevel(), sp, ParticleTypes.SOUL, 15);
        }));
        reg("death_7", new Marker("dea_spirit_ritual"));
        // 伪装活尸：清除 20 米内亡灵对自己的仇恨（亡灵不再主动攻击）
        reg("death_7", new Custom("dea_undead_disguise", 0, 5, sp -> {
            for (LivingEntity e : AbilityFx.around(sp, 20, AbilityFx.Filter.UNDEAD)) {
                if (e instanceof Mob mob && mob.getTarget() == sp) {
                    mob.setTarget(null);
                }
            }
            AbilityFx.burst(sp.serverLevel(), sp, ParticleTypes.SOUL, 10);
            AbilityFx.activated(sp, "ability.guimi_mod.dea_undead_disguise");
        }));
        reg("death_7", new Marker("dea_spirit_sense"));

        // ===== 死神 · 序列6 死灵导师 =====
        // 驱使亡灵：收编 10 米内至多 3 只亡灵为己方战力 2 分钟
        reg("death_6", new Custom("dea_command_undead", 20, 60, sp -> {
            int converted = 0;
            for (LivingEntity e : AbilityFx.around(sp, 10, AbilityFx.Filter.UNDEAD)) {
                if (converted >= 3 || !(e instanceof Mob mob)) continue;
                mob.addTag(Summon.SUMMON_TAG);
                mob.getPersistentData().putLong(Summon.DESPAWN_KEY,
                        sp.serverLevel().getGameTime() + 2400);
                mob.getPersistentData().putUUID(Summon.OWNER_KEY, sp.getUUID());
                mob.getPersistentData().putBoolean(Summon.HOSTILE_KEY, false);
                mob.setTarget(null);
                AbilityFx.burst(sp.serverLevel(), mob, ParticleTypes.SOUL, 15);
                converted++;
            }
            if (converted == 0) {
                AbilityFx.noTarget(sp);
                AbilityFx.refund(sp, 20);
                return;
            }
            AbilityFx.activated(sp, "ability.guimi_mod.dea_command_undead");
        }));
        // 复活：唤起活尸（持续 5 分钟）
        reg("death_6", new Summon("dea_raise_dead", 20, 30,
                () -> EntityType.ZOMBIE, 1, 300, 20, 3, 0F));
        reg("death_6", new Marker("dea_dead_speech"));
        reg("death_6", new Marker("dea_spirit_realm"));
    }
}