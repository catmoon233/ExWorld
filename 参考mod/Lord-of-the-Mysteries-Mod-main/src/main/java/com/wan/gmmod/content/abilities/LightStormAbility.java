package com.wan.gmmod.content.abilities;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.common.capability.data.CooldownData;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 光之风暴 —— 黄昏巨人「黎明骑士」序列 6 的最强攻击。
 * <p>
 * 将「晨曦之剑」刺入地面并令其崩解，化作数之不尽、闪烁着晨曦光辉的碎片，
 * 组成狂暴而锐利的光刃飓风，把笼罩范围内的一切阴影与邪异切割成碎片，
 * 可作为区域毁灭手段，直接斩碎形体、消除怨魂、创伤恶灵。
 * <p>
 * 行为说明：
 * <ul>
 *     <li>范围持续伤害：风暴在玩家四周维持片刻，周期性切割范围内所有活体；</li>
 *     <li>对亡灵（治疗反转者）附带额外伤害与灼烧（对应「悲灵 / 消灭恶灵」）；</li>
 *     <li>风眼可略受操控：随玩家视线方向小幅漂移（对应“一定程度上可控方向”）；</li>
 *     <li>风暴带有粒子动画（上升的晨曦碎屑 + 旋转的金色利刃残影）；</li>
 *     <li>使用后短暂时间内无法重新凝聚「晨曦之剑」（写入晨曦之剑的冷却）。</li>
 * </ul>
 */
public class LightStormAbility extends Ability {

    /** 额外对亡灵（治疗反转）目标施加的加成倍率 */
    private static final float UNDEAD_BONUS = 2.0F;
    /** 额外对亡灵 / 邪异目标点燃的刻数（圣光灼烧） */
    private static final int UNDEAD_FIRE_TICKS = 80;

    private final double radius;
    private final float perDamage;
    private final int totalTicks;
    private final int intervalTicks;
    /** 使用后锁定「晨曦之剑」的刻数（无法重新凝聚） */
    private final int swordLockTicks;

    /** 活跃中的风暴：玩家 UUID → 风暴状态（服务端维护）。 */
    private static final Map<UUID, Storm> ACTIVE = new HashMap<>();

    private static final class Storm {
        final ServerPlayer owner;
        Vec3 pos;
        int remaining;
        final double radius;
        final float perDamage;
        final int interval;

        Storm(ServerPlayer owner, Vec3 pos, int totalTicks, double radius, float perDamage, int interval) {
            this.owner = owner;
            this.pos = pos;
            this.remaining = totalTicks;
            this.radius = radius;
            this.perDamage = perDamage;
            this.interval = interval;
        }
    }

    public LightStormAbility(int cost, int cdSecs, double radius, float perDamage,
                             int totalTicks, int intervalTicks, int swordLockTicks) {
        super(GuimiMod.id("gia_light_storm"), cost, cdSecs * 20, true);
        this.radius = radius;
        this.perDamage = perDamage;
        this.totalTicks = totalTicks;
        this.intervalTicks = intervalTicks;
        this.swordLockTicks = swordLockTicks;
    }

    @Override
    public void onActivate(Player player) {
        if (!(player instanceof ServerPlayer sp)) return;
        ServerLevel level = sp.serverLevel();
        long now = level.getGameTime();

        // 记录自身冷却，并把「晨曦之剑」一并锁入冷却（短暂无法重新凝聚）
        CooldownData cd = sp.getData(ModAttachments.SKILL_COOLDOWNS)
                .with(GuimiMod.id("gia_light_storm"), now + cooldownTicks, now)
                .with(GuimiMod.id("gia_dawn_sword"), now + swordLockTicks, now);
        sp.setData(ModAttachments.SKILL_COOLDOWNS, cd);

        // 建立/刷新风暴（覆盖旧实例），从玩家位置升起
        ACTIVE.put(sp.getUUID(), new Storm(sp, sp.position(), totalTicks, radius, perDamage, intervalTicks));

        // 崩解起手动画：晨曦碎屑迸发 + 一道环形金光残影 + 崩解音效
        RandomSource r = sp.getRandom();
        level.sendParticles(ParticleTypes.END_ROD, sp.getX(), sp.getY() + 0.6, sp.getZ(),
                30, 1.5, 1.2, 1.5, 0.05);
        level.sendParticles(new DustParticleOptions(new org.joml.Vector3f(1.0F, 0.92F, 0.6F), 1.2F),
                sp.getX(), sp.getY() + 0.3, sp.getZ(), 24, 1.2, 0.4, 1.2, 0.02);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, sp.getX(), sp.getY() + 0.4, sp.getZ(),
                8, 0.5, 0.2, 0.5, 0.0);
        level.playSound(null, sp.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 0.7F);
        level.playSound(null, sp.blockPosition(), SoundEvents.ANVIL_HIT, SoundSource.PLAYERS, 0.9F, 1.3F);
        for (int i = 0; i < 5; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            Vec3 dir = new Vec3(Math.cos(a), 0, Math.sin(a));
            double d = radius * 0.6;
            level.sendParticles(ParticleTypes.END_ROD,
                    sp.getX() + dir.x * d, sp.getY() + 0.4, sp.getZ() + dir.z * d,
                    6, 0.25, 0.25, 0.25, 0.03);
        }

        AbilityFx.activated(sp, getNameKey());
    }

    @Override
    public void onDeactivate(Player player) {
        ACTIVE.remove(player.getUUID());
    }

    /**
     * 每 tick 推进一场风暴（服务端，由 PlayerTickEvent 驱动）。只处理风暴动画与周期性伤害。
     */
    public static void tick(ServerPlayer sp) {
        Storm s = ACTIVE.get(sp.getUUID());
        if (s == null) return;
        ServerLevel level = sp.serverLevel();
        if (sp.isRemoved() || !sp.isAlive()) {
            ACTIVE.remove(sp.getUUID());
            return;
        }
        RandomSource r = sp.getRandom();

        // 风眼轻微向玩家视线方向漂移（一定程度的可控方向），并钳制在以玩家为中心半径内
        Vec3 target = sp.getViewVector(1.0F).scale(s.radius * 0.9)
                .add(sp.getX(), sp.getY() + 0.6, sp.getZ());
        Vec3 drift = target.subtract(s.pos).scale(0.08);
        s.pos = s.pos.add(drift);

        // 动画：环形金色利刃残影（断断续续旋转） + 上下飘散的晨曦碎屑
        double phase = sp.tickCount * 0.6;
        for (int i = 0; i < Math.max(1, s.radius); i++) {
            double a = phase + i * (Math.PI * 2 / Math.max(1, (int) s.radius));
            Vec3 rot = new Vec3(Math.cos(a), 0, Math.sin(a));
            Vec3 px = s.pos.add(rot.scale(s.radius * (1.0 - 0.25 * (sp.tickCount % 3))));
            level.sendParticles(ParticleTypes.END_ROD,
                    px.x, px.y + 0.5 + Math.sin(a + sp.tickCount * 0.2) * 0.5, px.z,
                    1, 0.05, 0.1, 0.05, 0.0);
            level.sendParticles(new DustParticleOptions(new org.joml.Vector3f(1.0F, 0.9F, 0.55F), 1.0F),
                    px.x, px.y, px.z, 1, 0.05, 0.05, 0.05, 0.0);
        }
        // 垂直卷起的碎屑（动画感）
        for (int i = 0; i < 6; i++) {
            Vec3 base = s.pos.add(new Vec3(
                    (r.nextDouble() * 2 - 1) * s.radius,
                    0.5 + r.nextDouble() * s.radius * 0.5,
                    (r.nextDouble() * 2 - 1) * s.radius));
            level.sendParticles(i % 2 == 0 ? ParticleTypes.END_ROD : ParticleTypes.CRIT,
                    base.x, base.y, base.z, 1, 0.1, 0.6, 0.1, 0.02);
        }

        // 周期性切割伤害
        if (s.remaining % s.interval == 0) {
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
                    new net.minecraft.world.phys.AABB(s.pos.x - s.radius, s.pos.y - s.radius,
                            s.pos.z - s.radius, s.pos.x + s.radius, s.pos.y + s.radius, s.pos.z + s.radius),
                    e -> e != sp && e.isAlive());
            for (LivingEntity t : targets) {
                double dist = Math.max(0.1, t.distanceToSqr(s.pos));
                float dmg = s.perDamage * (1.0F - (float) (Math.sqrt(dist) / s.radius) * 0.4F);
                boolean undead = t.isInvertedHealAndHarm();
                float dealt = undead ? dmg * UNDEAD_BONUS : dmg;
                t.hurt(sp.damageSources().indirectMagic(sp, sp), dealt);
                if (undead && t.isAlive() && t.getRemainingFireTicks() < UNDEAD_FIRE_TICKS) {
                    t.setRemainingFireTicks(UNDEAD_FIRE_TICKS);
                }
                Vec3 away = t.position().subtract(s.pos);
                double len = away.length();
                if (len > 0.001) {
                    t.knockback(0.9, away.x / len, away.z / len);
                }
                AbilityFx.burst(level, t, ParticleTypes.END_ROD, 3);
            }
        }

        if (--s.remaining <= 0) {
            ACTIVE.remove(sp.getUUID());
            level.playSound(null, s.pos.x, s.pos.y + 0.5, s.pos.z,
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.2F, 1.4F);
            level.sendParticles(ParticleTypes.END_ROD, s.pos.x, s.pos.y + 0.5, s.pos.z,
                    40, s.radius, 0.5, s.radius, 0.05);
        }
    }

    /** 玩家登出时清理其风暴，避免占用内存。 */
    public static void cleanup(UUID uuid) {
        ACTIVE.remove(uuid);
    }
}