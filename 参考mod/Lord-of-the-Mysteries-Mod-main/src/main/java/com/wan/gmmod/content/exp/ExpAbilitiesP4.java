package com.wan.gmmod.content.exp;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.registry.ModEffects;
import com.wan.gmmod.common.registry.ModItems;
import com.wan.gmmod.content.abilities.Ability;
import com.wan.gmmod.content.abilities.AbilityRegistry;
import com.wan.gmmod.content.exp.ExpAbilityTypes.Aoe;
import com.wan.gmmod.content.exp.ExpAbilityTypes.Aura;
import com.wan.gmmod.content.exp.ExpAbilityTypes.Custom;
import com.wan.gmmod.content.exp.ExpAbilityTypes.Marker;
import com.wan.gmmod.content.exp.ExpAbilityTypes.PassiveEffect;
import com.wan.gmmod.content.exp.ExpAbilityTypes.SelfBuff;
import com.wan.gmmod.content.exp.ExpAbilityTypes.Summon;
import com.wan.gmmod.content.exp.ExpAbilityTypes.Target;
import com.wan.gmmod.content.exp.ExpAbilityTypes.TickPassive;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.wan.gmmod.content.exp.ExpFx.fx;

/**
 * 实验性途径能力注册 · 第四组：黑皇帝(be_) / 被缚者(cha_) / 深渊(aby_)。
 */
final class ExpAbilitiesP4 {

    private ExpAbilitiesP4() {}

    private static void reg(String seq, Ability ability) {
        AbilityRegistry.register(GuimiMod.id(seq), ability);
    }

    static void init() {
        // ===== 黑皇帝 · 序列9 律师 =====
        reg("black_emperor_9", new Marker("be_eloquence"));
        reg("black_emperor_9", new Marker("be_rule_loophole"));
        reg("black_emperor_9", new Marker("be_logic"));

        // ===== 黑皇帝 · 序列8 野蛮人 =====
        // 力量强化：力量大幅提升
        reg("black_emperor_8", new PassiveEffect("be_strength", fx(MobEffects.DAMAGE_BOOST, 1)));
        // 体魄：最大生命 + 自然恢复
        reg("black_emperor_8", new PassiveEffect("be_physique",
                fx(MobEffects.HEALTH_BOOST, 1), fx(MobEffects.REGENERATION, 0)));
        reg("black_emperor_8", new Marker("be_resist"));
        reg("black_emperor_8", new Marker("be_rule_defiance"));

        // ===== 黑皇帝 · 序列7 贿赂者 =====
        // 削弱：削弱目标攻击 / 防御 30 秒
        reg("black_emperor_7", new Target("be_weaken", 10, 60, 10, 0, 1, 30,
                fx(MobEffects.WEAKNESS, 1), fx(MobEffects.MOVEMENT_SLOWDOWN, 0)));
        // 魅惑：给予贿赂，目标不再想战斗
        reg("black_emperor_7", new Custom("be_bribe_charm", 10, 90, sp -> {
            List<LivingEntity> hits = ExpFx.rayTargets(sp, 10, 1);
            if (hits.isEmpty() || !(hits.get(0) instanceof Mob mob)) {
                ExpFx.noTarget(sp);
                ExpFx.refund(sp, 10);
                return;
            }
            mob.setTarget(null);
            ExpFx.apply(mob, List.of(fx(MobEffects.WEAKNESS, 0)), 600, false);
            ExpFx.burst(sp.serverLevel(), mob, ParticleTypes.HEART, 8);
            ExpFx.activated(sp, "ability.guimi_mod.be_bribe_charm");
        }));
        // 狂妄：目标骄傲自满 → 判断失误
        reg("black_emperor_7", new Target("be_arrogance", 10, 60, 10, 0, 1, 20,
                fx(MobEffects.CONFUSION, 0), fx(MobEffects.UNLUCK, 1)));
        // 关联：建立联系，可长时间定位目标
        reg("black_emperor_7", new Target("be_link", 5, 30, 10, 0, 1, 120,
                fx(MobEffects.GLOWING, 0)));

        // ===== 黑皇帝 · 序列6 腐化男爵 =====
        // 扭曲：扭曲目标攻击含义（伤害 + 虚弱 + 眩晕）
        reg("black_emperor_6", new Target("be_distort", 15, 45, 12, 4, 1, 10,
                fx(MobEffects.WEAKNESS, 2), fx(MobEffects.CONFUSION, 0)));
        // 腐蚀：10 米内生物心灵逐渐阴暗（霉运 + 虚弱光环）
        reg("black_emperor_6", new Aura("be_corrupt", 10, ExpFx.Filter.HOSTILE, false,
                fx(MobEffects.UNLUCK, 0), fx(MobEffects.WEAKNESS, 0)));
        reg("black_emperor_6", new Marker("be_rule_drain"));
        // 混乱导师：15 米内所有生物随机交换攻击目标
        reg("black_emperor_6", new Custom("be_chaos_master", 20, 90, sp -> {
            List<LivingEntity> targets = ExpFx.around(sp, 15, ExpFx.Filter.ALL);
            for (LivingEntity e : targets) {
                if (e instanceof Mob mob && !targets.isEmpty()) {
                    LivingEntity other = targets.get(sp.getRandom().nextInt(targets.size()));
                    mob.setTarget(other == mob ? null : other);
                    ExpFx.apply(mob, List.of(fx(MobEffects.CONFUSION, 0)), 300, false);
                }
            }
            ExpFx.burst(sp.serverLevel(), sp, ParticleTypes.WITCH, 40);
            ExpFx.activated(sp, "ability.guimi_mod.be_chaos_master");
        }));

        // ===== 被缚者 · 序列9 囚犯 =====
        // 强壮：力量 + 生命提升
        reg("chained_9", new PassiveEffect("cha_strong",
                fx(MobEffects.DAMAGE_BOOST, 0), fx(MobEffects.HEALTH_BOOST, 0)));
        // 知觉敏锐：黑暗中视物
        reg("chained_9", new PassiveEffect("cha_keen_sense", fx(MobEffects.NIGHT_VISION, 0)));
        // 犯罪技巧：顺手的家伙 + 开锁（急迫）
        reg("chained_9", new PassiveEffect("cha_crime_skill", fx(MobEffects.DIG_SPEED, 0)));
        reg("chained_9", new Marker("cha_suppress_desire"));

        // ===== 被缚者 · 序列8 疯子 =====
        // 主动疯狂：牺牲理智换取全属性大幅提升 30 秒
        reg("chained_8", new SelfBuff("cha_active_madness", 0, 90, 30, false, 15,
                fx(MobEffects.DAMAGE_BOOST, 1), fx(MobEffects.MOVEMENT_SPEED, 1),
                fx(MobEffects.DAMAGE_RESISTANCE, 0)));
        reg("chained_8", new Marker("cha_curse"));
        // 欲望爆发：生命低于 30% 时自动触发疯狂
        reg("chained_8", new TickPassive("cha_desire_burst", 40, sp -> {
            if (sp.getHealth() < sp.getMaxHealth() * 0.3F
                    && !sp.hasEffect(MobEffects.DAMAGE_BOOST)) {
                ExpFx.apply(sp, List.of(fx(MobEffects.DAMAGE_BOOST, 1),
                        fx(MobEffects.MOVEMENT_SPEED, 1)), 300, false);
                ExpFx.burst(sp.serverLevel(), sp, ParticleTypes.ANGRY_VILLAGER, 15);
            }
        }));
        reg("chained_8", new Marker("cha_mind_resist"));

        // ===== 被缚者 · 序列7 狼人 =====
        // 狼人化：力量 / 速度 / 自愈大幅提升 60 秒（外加变身标记驱动狼人化叠加模型）
        reg("chained_7", new SelfBuff("cha_werewolf", 20, 180, 60,
                fx(MobEffects.DAMAGE_BOOST, 1), fx(MobEffects.MOVEMENT_SPEED, 1),
                fx(MobEffects.REGENERATION, 1), fx(ModEffects.WEREWOLF_FORM, 0)));
        // 黑暗法术：毒性浸入 + 驱光恐惧（中毒 + 黑暗 + 缓慢）
        reg("chained_7", new Target("cha_dark_spell", 10, 30, 12, 2, 1, 8,
                fx(MobEffects.POISON, 0), fx(MobEffects.DARKNESS, 0),
                fx(MobEffects.MOVEMENT_SLOWDOWN, 0)));
        reg("chained_7", new Marker("cha_anti_divine"));
        // 超凡爪牙：空手时利爪自带增伤
        reg("chained_7", new TickPassive("cha_claws", 40, sp -> {
            if (sp.getMainHandItem().isEmpty()) {
                ExpFx.apply(sp, List.of(fx(MobEffects.DAMAGE_BOOST, 1)), 100, true);
            }
        }));

        // ===== 被缚者 · 序列6 活尸 =====
        // 钢铁之躯：由 ExpEventHandler 实现（物理减伤 + 免火 / 溺水）
        reg("chained_6", new Marker("cha_steel_body"));
        // 超速奔跑：奔跑速度极快
        reg("chained_6", new PassiveEffect("cha_super_speed", fx(MobEffects.MOVEMENT_SPEED, 1)));
        // 活尸力量：狂暴巨力
        reg("chained_6", new PassiveEffect("cha_zombie_strength", fx(MobEffects.DAMAGE_BOOST, 1)));
        // 冰霜掌控：接触冻结目标
        reg("chained_6", new Target("cha_frost_control", 10, 20, 3, 3, 1, 5,
                fx(MobEffects.MOVEMENT_SLOWDOWN, 4)));
        // 腐尸操控：复活死尸为傀儡
        reg("chained_6", new Summon("cha_corpse_control", 20, 60,
                () -> EntityType.ZOMBIE, 1, 120, 25, 5, 0F));
        reg("chained_6", new Marker("cha_death_spell"));
        // 满月诅咒：满月之夜极度痛苦（虚弱 + 缓慢）
        reg("chained_6", new TickPassive("cha_full_moon_curse", 100, sp -> {
            if (!sp.serverLevel().isDay() && sp.serverLevel().getMoonPhase() == 0) {
                ExpFx.apply(sp, List.of(fx(MobEffects.WEAKNESS, 1),
                        fx(MobEffects.MOVEMENT_SLOWDOWN, 0)), 120, true);
            }
        }));

        // ===== 深渊 · 序列9 罪犯 =====
        // 强壮身体：力量 + 生命提升
        reg("abyss_9", new PassiveEffect("aby_strong_body",
                fx(MobEffects.DAMAGE_BOOST, 0), fx(MobEffects.HEALTH_BOOST, 0)));
        reg("abyss_9", new Marker("aby_weapon_mastery"));
        // 敏锐直觉：锁定自己的敌人显形预警
        reg("abyss_9", new TickPassive("aby_keen_intuition", 60, sp -> {
            for (LivingEntity e : ExpFx.around(sp, 12, ExpFx.Filter.HOSTILE)) {
                if (e instanceof Mob mob && mob.getTarget() == sp) {
                    ExpFx.apply(e, List.of(fx(MobEffects.GLOWING, 0)), 80, true);
                }
            }
        }));
        reg("abyss_9", new Marker("aby_conscience"));

        // ===== 深渊 · 序列8 冷血者 =====
        // 非人之躯：由 ExpEventHandler 实现（物理伤害 -15%）
        reg("abyss_8", new Marker("aby_inhuman_body"));
        // 毒火：伤害 5 + 中毒
        reg("abyss_8", new Target("aby_poison_fire", 10, 15, 12, 5, 1, 6,
                fx(MobEffects.POISON, 0)));
        // 迟缓：目标移动速度 -40%，10 秒
        reg("abyss_8", new Target("aby_slowness", 8, 25, 12, 0, 1, 10,
                fx(MobEffects.MOVEMENT_SLOWDOWN, 1)));
        // 恶语伤人：弱小诅咒，目标攻击力下降
        reg("abyss_8", new Target("aby_evil_words", 5, 20, 12, 0, 1, 15,
                fx(MobEffects.WEAKNESS, 0)));
        reg("abyss_8", new Marker("aby_conscience_lost"));

        // ===== 深渊 · 序列7 连环杀手 =====
        reg("abyss_7", new Marker("aby_demon_ritual"));
        // 召唤恶魔投影：召唤深渊恶魔投影助战
        reg("abyss_7", new Summon("aby_demon_projection", 30, 300,
                () -> EntityType.VEX, 2, 60, 14, 6, 0F));
        reg("abyss_7", new Marker("aby_anti_divination"));
        reg("abyss_7", new Marker("aby_serial_expertise"));

        // ===== 深渊 · 序列6 恶魔 =====
        // 恶魔化：巨大化，全属性大幅提升 + 类飞行 45 秒（外加变身标记驱动恶魔化全身模型）
        reg("abyss_6", new SelfBuff("aby_demonize", 30, 300, 45,
                fx(MobEffects.DAMAGE_BOOST, 2), fx(MobEffects.MOVEMENT_SPEED, 1),
                fx(MobEffects.DAMAGE_RESISTANCE, 1), fx(MobEffects.JUMP, 2),
                fx(MobEffects.SLOW_FALLING, 0), fx(ModEffects.DEMON_FORM, 0)));
        // 恶意感知：致命威胁提前感知（锁定者显形）
        reg("abyss_6", new TickPassive("aby_malice_sense", 60, sp -> {
            for (LivingEntity e : ExpFx.around(sp, 20, ExpFx.Filter.ALL)) {
                if (e instanceof Mob mob && mob.getTarget() == sp) {
                    ExpFx.apply(e, List.of(fx(MobEffects.GLOWING, 0)), 80, true);
                }
            }
        }));
        // 岩浆之剑：恶魔获得标志性武器岩浆之剑（手持伤害 + 点燃）
        reg("abyss_6", new Custom("aby_magma_sword", 15, 30, sp -> {
            ItemStack sword = new ItemStack(ModItems.MAGMA_SWORD.get());
            if (!sp.addItem(sword.copy())) {
                sp.drop(sword, false);
            }
            ExpFx.burst(sp.serverLevel(), sp, ParticleTypes.LAVA, 16);
            ExpFx.activated(sp, "ability.guimi_mod.aby_magma_sword");
        }));
        // 硫磺火球：淡蓝火球，伤害 7 + 中毒
        reg("abyss_6", new Target("aby_sulfur_fireball", 10, 10, 20, 7, 1, 5,
                fx(MobEffects.POISON, 0)));
        // 火焰囚笼：禁锢目标 5 秒
        reg("abyss_6", new Target("aby_flame_cage", 12, 25, 12, 0, 1, 5,
                fx(MobEffects.MOVEMENT_SLOWDOWN, 6)));
        // 污秽之语·缓慢：8 米内所有目标僵硬停滞 2 秒
        reg("abyss_6", new Aoe("aby_foul_slow", 10, 30, 8, ExpFx.Filter.HOSTILE,
                false, 0, 0, false, 2, fx(MobEffects.MOVEMENT_SLOWDOWN, 5)));
        // 污秽之语·死：攥住目标心脏，濒死重伤
        reg("abyss_6", new Target("aby_foul_death", 40, 120, 10, 19, 1, 0));
        // 污秽之语·堕落：范围攻击——目标及周围区域深渊化，体表覆盖粘稠黑液并炸开化为堕落黑雾，克制替身
        reg("abyss_6", new Custom("aby_foul_corrupt", 25, 60, sp -> {
            List<LivingEntity> hits = ExpFx.rayTargets(sp, 12, 1);
            if (hits.isEmpty()) {
                ExpFx.noTarget(sp);
                ExpFx.refund(sp, 25);
                return;
            }
            Vec3 center = hits.get(0).position();
            ServerLevel level = sp.serverLevel();
            List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class,
                    new AABB(center, center).inflate(5.0),
                    e -> e != sp && ExpFx.Filter.HOSTILE.test(sp, e));
            for (LivingEntity e : victims) {
                e.hurt(sp.damageSources().indirectMagic(sp, sp), 8.0F);
                // 深渊化：目标与周围区域出现深渊化倾向（期间替身类能力失效）
                e.addEffect(new MobEffectInstance(ModEffects.FALL_CORRUPTION, 160, 0, false, true, true));
                // 体表覆盖上一层略显粘稠的黑色液体
                level.sendParticles(ParticleTypes.SQUID_INK,
                        e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(),
                        8, e.getBbWidth() * 0.5, e.getBbHeight() * 0.4, e.getBbWidth() * 0.5, 0.0);
            }
            // 黑液炸开破碎、化为堕落的黑雾
            level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z, 4, 0.6, 0.6, 0.6, 0.0);
            level.sendParticles(ParticleTypes.SQUID_INK, center.x, center.y, center.z, 24, 5.0, 1.0, 5.0, 0.05);
            level.sendParticles(ParticleTypes.SMOKE, center.x, center.y, center.z, 40, 5.0, 0.6, 5.0, 0.08);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, center.x, center.y, center.z, 18, 3.0, 0.8, 3.0, 0.04);
            level.sendParticles(ParticleTypes.SCULK_SOUL, center.x, center.y, center.z, 14, 5.0, 0.5, 5.0, 0.02);
            level.playSound(null, BlockPos.containing(center),
                    SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.5F, 1.0F);
            ExpFx.activated(sp, "ability.guimi_mod.aby_foul_corrupt");
        }));
        // 免疫：常态化解毒素并抗火
        reg("abyss_6", new TickPassive("aby_immunity", 40, sp -> {
            sp.removeEffect(MobEffects.POISON);
            ExpFx.apply(sp, List.of(fx(MobEffects.FIRE_RESISTANCE, 0)), 100, true);
        }));
        reg("abyss_6", new Marker("aby_cold_blood"));
        // 失控风险：偶发心智动摇（反胃 + 理智流失）
        reg("abyss_6", new TickPassive("aby_lose_control", 1200, sp -> {
            if (sp.getRandom().nextFloat() < 0.10F) {
                ExpFx.apply(sp, List.of(fx(MobEffects.CONFUSION, 0)), 100, true);
                ExpFx.addSanity(sp, -1);
            }
        }));
    }
}
