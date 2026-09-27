package com.wan.gmmod.content.exp;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.item.CharacteristicItem;
import com.wan.gmmod.common.item.SealedArtifactItem;
import com.wan.gmmod.content.abilities.Ability;
import com.wan.gmmod.content.abilities.AbilityRegistry;
import com.wan.gmmod.content.characteristics.CharacteristicData;
import com.wan.gmmod.content.exp.ExpAbilityTypes.Aoe;
import com.wan.gmmod.content.exp.ExpAbilityTypes.Custom;
import com.wan.gmmod.content.exp.ExpAbilityTypes.Marker;
import com.wan.gmmod.content.exp.ExpAbilityTypes.PassiveEffect;
import com.wan.gmmod.content.exp.ExpAbilityTypes.SelfBuff;
import com.wan.gmmod.content.exp.ExpAbilityTypes.Target;
import com.wan.gmmod.content.exp.ExpAbilityTypes.TickPassive;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.List;

import static com.wan.gmmod.content.exp.ExpFx.fx;

/**
 * 实验性途径能力注册 · 第三组：完美者(par_) / 隐者(her_) / 命运之轮(whl_) / 审判者(jus_)。
 */
final class ExpAbilitiesP3 {

    private ExpAbilitiesP3() {}

    private static void reg(String seq, Ability ability) {
        AbilityRegistry.register(GuimiMod.id(seq), ability);
    }

    static void init() {
        // ===== 完美者 · 序列9 通识者 =====
        reg("paragon_9", new Marker("par_erudition"));

        // ===== 完美者 · 序列8 考古学家 =====
        reg("paragon_8", new Marker("par_history"));
        // 野外生存：饱食度下降缓慢（周期性少量补充）
        reg("paragon_8", new TickPassive("par_survival", 1200,
                sp -> sp.getFoodData().eat(1, 0.5F)));
        reg("paragon_8", new Marker("par_ritual"));
        // 体魄强化：生命 + 力量提升
        reg("paragon_8", new PassiveEffect("par_body_boost",
                fx(MobEffects.HEALTH_BOOST, 0), fx(MobEffects.DAMAGE_BOOST, 0)));

        // ===== 完美者 · 序列7 鉴定师 =====
        // 鉴定：快速鉴定手中物品
        reg("paragon_7", new Custom("par_identify", 5, 10, sp -> {
            var stack = sp.getMainHandItem();
            if (stack.isEmpty()) {
                ExpFx.noTarget(sp);
                ExpFx.refund(sp, 5);
                return;
            }
            sp.sendSystemMessage(Component.translatable("message.guimi_mod.exp.identify",
                    stack.getHoverName(), stack.isEnchanted()
                            ? Component.translatable("message.guimi_mod.exp.identify.magic")
                            : Component.translatable("message.guimi_mod.exp.identify.plain")));
        }));
        reg("paragon_7", new Marker("par_item_optimize"));
        reg("paragon_7", new Marker("par_spirit_sense"));

        // ===== 完美者 · 序列6 机械专家 =====
        reg("paragon_6", new Marker("par_mechanics"));
        reg("paragon_6", new Marker("par_ritual_enhance"));
        // 机械操控：暂时强化手中装置（急迫大幅提升 30 秒）
        reg("paragon_6", new SelfBuff("par_machine_control", 10, 60, 30, fx(MobEffects.DIG_SPEED, 1)));
        reg("paragon_6", new Marker("par_precision"));
        // 工匠封印：制造非凡物品——主手持耐久装备、副手持一份非凡特性 → 封印成封印物
        reg("paragon_6", new Custom("par_seal_artifact", 15, 5, sp -> {
            ItemStack base = sp.getMainHandItem();
            ItemStack trait = sp.getOffhandItem();
            if (!SealedArtifactItem.isSealableBase(base)
                    || !SealedArtifactItem.isCharacteristic(trait)) {
                sp.displayClientMessage(
                        Component.translatable("message.guimi_mod.exp.seal_need_materials"), true);
                ExpFx.refund(sp, 15);
                return;
            }
            CharacteristicData data = CharacteristicItem.getData(trait);
            if (data == null) {
                ExpFx.refund(sp, 15);
                return;
            }
            ItemStack result = SealedArtifactItem.create(base, data);
            trait.shrink(1);
            sp.setItemInHand(InteractionHand.MAIN_HAND, result);
            sp.serverLevel().playSound(null, sp.blockPosition(),
                    SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.9F, 1.1F);
            ExpFx.burst(sp.serverLevel(), sp, ParticleTypes.ENCHANT, 24);
            ExpFx.activated(sp, "ability.guimi_mod.par_seal_artifact");
        }));

        // ===== 隐者 · 序列9 窥秘人 =====
        // 窥秘之眼：看穿隐藏存在（隐身生物显形）
        reg("hermit_9", new TickPassive("her_secret_eye", 60, sp -> {
            for (LivingEntity e : ExpFx.around(sp, 15, ExpFx.Filter.ALL)) {
                if (e.isInvisible()) {
                    ExpFx.apply(e, List.of(fx(MobEffects.GLOWING, 0)), 80, true);
                }
            }
        }));
        reg("hermit_9", new Marker("her_quick_ritual"));
        reg("hermit_9", new Marker("her_mystic_knowledge"));
        reg("hermit_9", new Marker("her_inspiration"));

        // ===== 隐者 · 序列8 格斗学者 =====
        reg("hermit_8", new PassiveEffect("her_combat_mastery", fx(MobEffects.DAMAGE_BOOST, 0)));
        // 体能强化：力量 + 速度提升
        reg("hermit_8", new PassiveEffect("her_body_boost",
                fx(MobEffects.MOVEMENT_SPEED, 0), fx(MobEffects.DAMAGE_BOOST, 0)));
        reg("hermit_8", new Marker("her_knowledge_pursuit"));
        reg("hermit_8", new Marker("her_spirit_expand"));

        // ===== 隐者 · 序列7 巫师 =====
        reg("hermit_7", new Marker("her_spell_learning"));
        // 照明术：制造光球（自身发光 + 夜视 60 秒）
        reg("hermit_7", new SelfBuff("her_light_spell", 3, 2, 60,
                fx(MobEffects.GLOWING, 0), fx(MobEffects.NIGHT_VISION, 0)));
        // 闪电术：落雷 + 麻痹 3 秒
        reg("hermit_7", new Custom("her_lightning_spell", 10, 15, sp -> {
            List<LivingEntity> hits = ExpFx.rayTargets(sp, 20, 1);
            if (hits.isEmpty()) {
                ExpFx.noTarget(sp);
                ExpFx.refund(sp, 10);
                return;
            }
            LivingEntity target = hits.get(0);
            var bolt = EntityType.LIGHTNING_BOLT.create(sp.serverLevel());
            if (bolt != null) {
                bolt.moveTo(target.getX(), target.getY(), target.getZ());
                bolt.setCause(sp);
                sp.serverLevel().addFreshEntity(bolt);
            }
            ExpFx.apply(target, List.of(fx(MobEffects.MOVEMENT_SLOWDOWN, 5)), 60, false);
            ExpFx.activated(sp, "ability.guimi_mod.her_lightning_spell");
        }));
        // 造风术：击退周围目标
        reg("hermit_7", new Aoe("her_wind_spell", 8, 10, 6, ExpFx.Filter.ALL,
                false, 0, 0, true, 0));
        // 力场之手：将 5 米内目标拉向自己
        reg("hermit_7", new Custom("her_force_hand", 5, 8, sp -> {
            List<LivingEntity> hits = ExpFx.rayTargets(sp, 5, 1);
            if (hits.isEmpty()) {
                ExpFx.noTarget(sp);
                ExpFx.refund(sp, 5);
                return;
            }
            LivingEntity target = hits.get(0);
            var pull = sp.position().subtract(target.position()).normalize().scale(0.8);
            target.setDeltaMovement(pull.x, 0.3, pull.z);
            target.hurtMarked = true;
            ExpFx.burst(sp.serverLevel(), target, ParticleTypes.ENCHANT, 12);
            ExpFx.activated(sp, "ability.guimi_mod.her_force_hand");
        }));
        // 冰冻术：目标减速 50% 持续 5 秒
        reg("hermit_7", new Target("her_freeze_spell", 8, 12, 12, 0, 1, 5,
                fx(MobEffects.MOVEMENT_SLOWDOWN, 2)));
        // 驱邪术：驱散亡灵，伤害 4
        reg("hermit_7", new Aoe("her_exorcism", 8, 20, 6, ExpFx.Filter.UNDEAD,
                false, 4, 0, true, 0));
        reg("hermit_7", new Marker("her_spellcasting"));

        // ===== 隐者 · 序列6 卷轴教授 =====
        reg("hermit_6", new Marker("her_scroll_making"));
        reg("hermit_6", new Marker("her_scroll_release"));
        // 冰冻强化：投掷冰晶，冻结 + 长减速
        reg("hermit_6", new Target("her_frost_enhance", 12, 25, 15, 3, 1, 10,
                fx(MobEffects.MOVEMENT_SLOWDOWN, 4)));
        // 麻痹：目标 3 秒无法行动
        reg("hermit_6", new Target("her_paralyze", 10, 20, 15, 0, 1, 3,
                fx(MobEffects.MOVEMENT_SLOWDOWN, 6), fx(MobEffects.WEAKNESS, 2)));
        reg("hermit_6", new Marker("her_spell_expand"));

        // ===== 命运之轮 · 序列9 怪物 =====
        // 灵感超高：偶尔听到他人听不见的低语
        reg("wheel_9", new TickPassive("whl_high_inspiration", 1200, sp -> {
            if (sp.getRandom().nextFloat() < 0.10F) {
                ExpFx.whisper(sp);
            }
        }));
        // 危险直觉：锁定自己的敌人提前显形预警
        reg("wheel_9", new TickPassive("whl_danger_intuition", 60, sp -> {
            for (LivingEntity e : ExpFx.around(sp, 12, ExpFx.Filter.HOSTILE)) {
                if (e instanceof Mob mob && mob.getTarget() == sp) {
                    ExpFx.apply(e, List.of(fx(MobEffects.GLOWING, 0)), 80, true);
                }
            }
        }));
        // 恍惚呓语：偶尔呢喃听不懂的话语
        reg("wheel_9", new TickPassive("whl_trance_murmur", 2400, sp -> {
            if (sp.getRandom().nextFloat() < 0.30F) {
                ExpFx.whisper(sp);
            }
        }));
        // 命运波动：幸运值随机波动
        reg("wheel_9", new TickPassive("whl_fate_wave", 600, sp ->
                ExpFx.apply(sp, List.of(fx(sp.getRandom().nextBoolean()
                        ? MobEffects.LUCK : MobEffects.UNLUCK, 0)), 600, true)));

        // ===== 命运之轮 · 序列8 机器 =====
        reg("wheel_8", new Marker("whl_calculation"));
        // 占卜：探知最近敌意存在的方位（使其显形），无所获时得到低语
        reg("wheel_8", new Custom("whl_divination", 5, 15, sp -> {
            LivingEntity nearest = ExpFx.around(sp, 30, ExpFx.Filter.HOSTILE).stream()
                    .min(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(sp)))
                    .orElse(null);
            if (nearest != null) {
                ExpFx.apply(nearest, List.of(fx(MobEffects.GLOWING, 0)), 200, false);
            } else {
                ExpFx.whisper(sp);
            }
            ExpFx.activated(sp, "ability.guimi_mod.whl_divination");
        }));
        reg("wheel_8", new Marker("whl_anti_divination"));
        // 体能强化：全属性提升
        reg("wheel_8", new PassiveEffect("whl_body_boost",
                fx(MobEffects.DAMAGE_BOOST, 0), fx(MobEffects.MOVEMENT_SPEED, 0)));

        // ===== 命运之轮 · 序列7 幸运儿 =====
        reg("wheel_7", new PassiveEffect("whl_luck", fx(MobEffects.LUCK, 0)));
        reg("wheel_7", new Marker("whl_probability_shift"));
        reg("wheel_7", new Marker("whl_luck_intuition"));
        reg("wheel_7", new Marker("whl_temperance"));

        // ===== 命运之轮 · 序列6 灾祸教士 =====
        reg("wheel_6", new Marker("whl_disaster_foresee"));
        // 灾祸引导：引灾波及目标（伤害 + 霉运 + 虚弱）
        reg("wheel_6", new Target("whl_disaster_guide", 20, 90, 20, 6, 1, 10,
                fx(MobEffects.UNLUCK, 2), fx(MobEffects.WEAKNESS, 1)));
        // 精神风暴：冲击目标精神体（眩晕 + 迷失）
        reg("wheel_6", new Target("whl_mind_storm", 15, 45, 15, 3, 1, 6,
                fx(MobEffects.CONFUSION, 0), fx(MobEffects.MOVEMENT_SLOWDOWN, 2)));
        // 幸运特长：战斗幸运大幅提升
        reg("wheel_6", new PassiveEffect("whl_luck_specialty", fx(MobEffects.LUCK, 1)));

        // ===== 审判者 · 序列9 仲裁人 =====
        // 权威：10 米内敌意生物有概率自动中立
        reg("justice_9", new TickPassive("jus_authority", 100, sp -> {
            for (LivingEntity e : ExpFx.around(sp, 10, ExpFx.Filter.HOSTILE)) {
                if (e instanceof Mob mob && mob.getTarget() == sp
                        && sp.getRandom().nextFloat() < 0.10F) {
                    mob.setTarget(null);
                }
            }
        }));
        reg("justice_9", new Marker("jus_charisma"));
        reg("justice_9", new PassiveEffect("jus_combat_mastery", fx(MobEffects.DAMAGE_BOOST, 0)));
        // 秩序感知：感知 10 米内混乱 / 邪恶事物（敌对高亮）
        reg("justice_9", new TickPassive("jus_order_sense", 100, sp -> {
            for (LivingEntity e : ExpFx.around(sp, 10, ExpFx.Filter.HOSTILE)) {
                ExpFx.apply(e, List.of(fx(MobEffects.GLOWING, 0)), 120, true);
            }
        }));

        // ===== 审判者 · 序列8 治安官 =====
        // 辖区：宣告辖区，长时间获得全属性提升
        reg("justice_8", new SelfBuff("jus_jurisdiction", 20, 300, 120,
                fx(MobEffects.DAMAGE_BOOST, 0), fx(MobEffects.DAMAGE_RESISTANCE, 0),
                fx(MobEffects.MOVEMENT_SPEED, 0)));
        reg("justice_8", new Marker("jus_trace_scout"));
        // 监控直觉：被监视 / 跟踪（被锁定）时预警
        reg("justice_8", new TickPassive("jus_monitor_intuition", 100, sp -> {
            for (LivingEntity e : ExpFx.around(sp, 20, ExpFx.Filter.ALL)) {
                if (e instanceof Mob mob && mob.getTarget() == sp) {
                    ExpFx.apply(e, List.of(fx(MobEffects.GLOWING, 0)), 120, true);
                }
            }
        }));
        reg("justice_8", new Marker("jus_weapon_mastery"));

        // ===== 审判者 · 序列7 审讯者 =====
        // 精神刺穿：无视肉身穿刺 5 米内灵体（伤害 + 剧痛缴械）
        reg("justice_7", new Aoe("jus_mind_pierce", 10, 20, 5, ExpFx.Filter.HOSTILE,
                false, 4, 0, false, 3, fx(MobEffects.WEAKNESS, 2)));
        // 痛苦之鞭：鞭打精神体，持续痛苦
        reg("justice_7", new Target("jus_pain_whip", 10, 25, 10, 3, 1, 8,
                fx(MobEffects.CONFUSION, 0)));
        // 刑具召唤：虚幻刑具直击灵体（需接近）
        reg("justice_7", new Target("jus_torture_summon", 10, 15, 4, 6, 1, 0));
        reg("justice_7", new Marker("jus_blast_mastery"));
        // 体质强化：生命 + 力量提升
        reg("justice_7", new PassiveEffect("jus_constitution",
                fx(MobEffects.HEALTH_BOOST, 0), fx(MobEffects.DAMAGE_BOOST, 0)));

        // ===== 审判者 · 序列6 法官 =====
        // 禁止：范围内强制遵守禁令（压制行动能力）
        reg("justice_6", new Aoe("jus_forbid", 15, 60, 4, ExpFx.Filter.HOSTILE,
                false, 0, 0, false, 10, fx(MobEffects.MOVEMENT_SLOWDOWN, 4), fx(MobEffects.WEAKNESS, 2)));
        // 囚禁：物理 + 灵体双重牢笼禁锢目标
        reg("justice_6", new Target("jus_imprison", 20, 90, 12, 0, 1, 5,
                fx(MobEffects.MOVEMENT_SLOWDOWN, 6), fx(MobEffects.WEAKNESS, 4)));
        // 鞭打：无形软鞭抽打
        reg("justice_6", new Target("jus_whip", 8, 15, 10, 4, 1, 3,
                fx(MobEffects.CONFUSION, 0)));
        // 死亡：宣判死亡，高额重击（约三倍斩击）
        reg("justice_6", new Target("jus_death_sentence", 30, 120, 15, 18, 1, 0));
    }
}
