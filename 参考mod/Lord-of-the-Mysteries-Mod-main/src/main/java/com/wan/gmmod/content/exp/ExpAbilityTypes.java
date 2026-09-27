package com.wan.gmmod.content.exp;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.content.abilities.Ability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 实验性途径能力的参数化通用类型。
 * <p>
 * 19 条实验途径共 300+ 个技能，绝大多数机制可归纳为以下几类模板，
 * 通过参数（半径 / 伤害 / 效果 / 目标筛选……）在 {@code ExpAbilities*} 注册表中批量实例化；
 * 无法归纳的少数技能使用 {@link Custom} / {@link TickPassive} 以 lambda 内联实现。
 * <p>
 * 数值近似约定：力量→伤害提升、体质→生命提升、恐惧→黑暗+缓慢、束缚→高级缓慢、
 * 眩晕→缓慢+反胃、灼烧→点燃；「+X%」类被动统一以最接近档位的原版药水效果表达。
 */
public final class ExpAbilityTypes {

    private ExpAbilityTypes() {}

    /** 标记被动：无 tick 逻辑，仅声明玩家拥有该能力（供事件钩子 / 后续机制查询）。 */
    public static class Marker extends Ability {
        public Marker(String path) {
            super(GuimiMod.id(path));
        }
    }

    /** 常驻效果被动：周期性为自身刷新隐藏药水效果。 */
    public static class PassiveEffect extends Ability {
        private final int interval;
        private final List<ExpFx.Effect> effects;

        public PassiveEffect(String path, ExpFx.Effect... effects) {
            super(GuimiMod.id(path));
            this.interval = 40;
            this.effects = List.of(effects);
        }

        @Override
        public void onPassiveTick(Player player) {
            if (player.level().isClientSide || player.tickCount % interval != 0) return;
            ExpFx.apply(player, effects, interval + 60, true);
        }
    }

    /** lambda 被动：每 interval 刻在服务端执行一次自定义逻辑。 */
    public static class TickPassive extends Ability {
        private final int interval;
        private final Consumer<ServerPlayer> action;

        public TickPassive(String path, int interval, Consumer<ServerPlayer> action) {
            super(GuimiMod.id(path));
            this.interval = interval;
            this.action = action;
        }

        @Override
        public void onPassiveTick(Player player) {
            if (player.level().isClientSide || player.tickCount % interval != 0) return;
            if (player instanceof ServerPlayer sp) action.accept(sp);
        }
    }

    /** 光环被动：周期性为半径内符合筛选的目标（可含自身）施加效果。 */
    public static class Aura extends Ability {
        private final double radius;
        private final ExpFx.Filter filter;
        private final boolean includeSelf;
        private final List<ExpFx.Effect> effects;

        public Aura(String path, double radius, ExpFx.Filter filter, boolean includeSelf,
                    ExpFx.Effect... effects) {
            super(GuimiMod.id(path));
            this.radius = radius;
            this.filter = filter;
            this.includeSelf = includeSelf;
            this.effects = List.of(effects);
        }

        @Override
        public void onPassiveTick(Player player) {
            if (player.level().isClientSide || player.tickCount % 40 != 0) return;
            if (!(player instanceof ServerPlayer sp)) return;
            if (includeSelf) ExpFx.apply(sp, effects, 100, true);
            for (LivingEntity e : ExpFx.around(sp, radius, filter)) {
                ExpFx.apply(e, effects, 100, true);
            }
        }
    }

    /** 主动自增益：为自身施加一组可见效果，可要求处于暗处 / 附带理智消耗。 */
    public static class SelfBuff extends Ability {
        private final int durationTicks;
        private final boolean requiresDarkness;
        private final int sanityCost;
        private final List<ExpFx.Effect> effects;

        public SelfBuff(String path, int cost, int cdSecs, int durationSecs, ExpFx.Effect... effects) {
            this(path, cost, cdSecs, durationSecs, false, 0, effects);
        }

        public SelfBuff(String path, int cost, int cdSecs, int durationSecs,
                        boolean requiresDarkness, int sanityCost, ExpFx.Effect... effects) {
            super(GuimiMod.id(path), cost, cdSecs * 20, true);
            this.durationTicks = durationSecs * 20;
            this.requiresDarkness = requiresDarkness;
            this.sanityCost = sanityCost;
            this.effects = effects == null ? List.of() : List.of(effects);
        }

        @Override
        public void onActivate(Player player) {
            if (!(player instanceof ServerPlayer sp)) return;
            if (requiresDarkness && !ExpFx.inDarkness(sp)) {
                sp.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("message.guimi_mod.exp.need_darkness"), true);
                ExpFx.refund(sp, getSpiritualityCost());
                return;
            }
            if (sanityCost > 0) {
                sp.setData(ModAttachments.SANITY,
                        Math.max(0, sp.getData(ModAttachments.SANITY) - sanityCost));
            }
            ExpFx.apply(sp, effects, durationTicks, false);
            ExpFx.burst(sp.serverLevel(), sp, ParticleTypes.ENCHANT, 20);
            ExpFx.activated(sp, getNameKey());
        }
    }

    /** 主动单体 / 穿透打击：视线射线选目标，造成魔法伤害并附加效果。 */
    public static class Target extends Ability {
        private final double range;
        private final float damage;
        private final int pierce;
        private final int durationTicks;
        private final List<ExpFx.Effect> effects;

        public Target(String path, int cost, int cdSecs, double range, float damage,
                      int pierce, int durationSecs, ExpFx.Effect... effects) {
            super(GuimiMod.id(path), cost, cdSecs * 20, true);
            this.range = range;
            this.damage = damage;
            this.pierce = Math.max(1, pierce);
            this.durationTicks = durationSecs * 20;
            this.effects = effects == null ? List.of() : List.of(effects);
        }

        @Override
        public void onActivate(Player player) {
            if (!(player instanceof ServerPlayer sp)) return;
            List<LivingEntity> hits = ExpFx.rayTargets(sp, range, pierce);
            if (hits.isEmpty()) {
                ExpFx.noTarget(sp);
                ExpFx.refund(sp, getSpiritualityCost());
                return;
            }
            for (LivingEntity e : hits) {
                if (damage > 0) e.hurt(sp.damageSources().indirectMagic(sp, sp), damage);
                ExpFx.apply(e, effects, durationTicks, false);
                ExpFx.burst(sp.serverLevel(), e, ParticleTypes.WITCH, 12);
            }
            sp.serverLevel().playSound(null, sp.blockPosition(),
                    SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.8F, 1.2F);
            ExpFx.activated(sp, getNameKey());
        }
    }

    /** 主动范围作用：对半径内符合筛选的目标造成伤害 / 治疗 / 效果 / 击退。 */
    public static class Aoe extends Ability {
        private final double radius;
        private final ExpFx.Filter filter;
        private final boolean includeSelf;
        private final float damage;
        private final float heal;
        private final boolean knockback;
        private final int durationTicks;
        private final List<ExpFx.Effect> effects;

        public Aoe(String path, int cost, int cdSecs, double radius, ExpFx.Filter filter,
                   boolean includeSelf, float damage, float heal, boolean knockback,
                   int durationSecs, ExpFx.Effect... effects) {
            super(GuimiMod.id(path), cost, cdSecs * 20, true);
            this.radius = radius;
            this.filter = filter;
            this.includeSelf = includeSelf;
            this.damage = damage;
            this.heal = heal;
            this.knockback = knockback;
            this.durationTicks = durationSecs * 20;
            this.effects = effects == null ? List.of() : List.of(effects);
        }

        @Override
        public void onActivate(Player player) {
            if (!(player instanceof ServerPlayer sp)) return;
            ServerLevel level = sp.serverLevel();
            if (includeSelf) {
                if (heal > 0) sp.heal(heal);
                ExpFx.apply(sp, effects, durationTicks, false);
            }
            for (LivingEntity e : ExpFx.around(sp, radius, filter)) {
                if (damage > 0) e.hurt(sp.damageSources().indirectMagic(sp, sp), damage);
                if (heal > 0) e.heal(heal);
                if (knockback) e.knockback(0.8, sp.getX() - e.getX(), sp.getZ() - e.getZ());
                ExpFx.apply(e, effects, durationTicks, false);
                ExpFx.burst(level, e, ParticleTypes.WITCH, 10);
            }
            ExpFx.burst(level, sp, ParticleTypes.END_ROD, 24);
            level.playSound(null, sp.blockPosition(),
                    SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.9F, 0.9F);
            ExpFx.activated(sp, getNameKey());
        }
    }

    /** 主动召唤：在身边召唤临时仆从（限时消散；可按概率召出敌对个体）。 */
    public static class Summon extends Ability {
        /** 召唤物统一标签：由 {@link ExpEventHandler} 驱动到期消散 / 目标维护。 */
        public static final String SUMMON_TAG = "gmmod_exp_summon";
        public static final String DESPAWN_KEY = "gmmod_exp_despawn";
        public static final String OWNER_KEY = "gmmod_exp_owner";
        public static final String HOSTILE_KEY = "gmmod_exp_hostile";
        /** 血仆转化标签：到期后恢复原状，期间跟随并攻击主人攻击的目标。 */
        public static final String BLOOD_SERVANT_TAG = "gmmod_exp_blood_servant";
        /** 血仆转化到期时刻（游戏刻）。 */
        public static final String BLOOD_EXPIRE_KEY = "gmmod_exp_blood_expire";

        private final Supplier<? extends EntityType<? extends Mob>> type;
        private final int count;
        private final int lifetimeTicks;
        private final double health;
        private final double attack;
        private final float hostileChance;

        public Summon(String path, int cost, int cdSecs,
                      Supplier<? extends EntityType<? extends Mob>> type, int count,
                      int lifetimeSecs, double health, double attack, float hostileChance) {
            super(GuimiMod.id(path), cost, cdSecs * 20, true);
            this.type = type;
            this.count = count;
            this.lifetimeTicks = lifetimeSecs * 20;
            this.health = health;
            this.attack = attack;
            this.hostileChance = hostileChance;
        }

        @Override
        public void onActivate(Player player) {
            if (!(player instanceof ServerPlayer sp)) return;
            spawn(sp, type.get(), count, lifetimeTicks, health, attack, hostileChance);
            sp.serverLevel().playSound(null, sp.blockPosition(),
                    SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 1.0F, 1.0F);
            ExpFx.activated(sp, getNameKey());
        }

        /** 召唤逻辑的静态入口，供 lambda 能力（如需消耗材料的血肉仆役）复用。 */
        public static void spawn(ServerPlayer sp, EntityType<? extends Mob> entityType, int count,
                                 int lifetimeTicks, double health, double attack, float hostileChance) {
            ServerLevel level = sp.serverLevel();
            RandomSource random = level.getRandom();
            for (int i = 0; i < count; i++) {
                Mob mob = entityType.create(level);
                if (mob == null) continue;
                double angle = random.nextDouble() * Math.PI * 2;
                mob.moveTo(sp.getX() + Math.cos(angle) * 2, sp.getY(), sp.getZ() + Math.sin(angle) * 2,
                        random.nextFloat() * 360F, 0);
                if (mob.getAttribute(Attributes.MAX_HEALTH) != null && health > 0) {
                    mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
                    mob.setHealth((float) health);
                }
                if (mob.getAttribute(Attributes.ATTACK_DAMAGE) != null && attack > 0) {
                    mob.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(attack);
                }
                mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()),
                        MobSpawnType.MOB_SUMMONED, null);
                mob.addTag(SUMMON_TAG);
                mob.getPersistentData().putLong(DESPAWN_KEY, level.getGameTime() + lifetimeTicks);
                mob.getPersistentData().putUUID(OWNER_KEY, sp.getUUID());
                boolean hostile = random.nextFloat() < hostileChance;
                mob.getPersistentData().putBoolean(HOSTILE_KEY, hostile);
                if (hostile) mob.setTarget(sp);
                mob.setPersistenceRequired();
                level.addFreshEntity(mob);
                ExpFx.burst(level, mob, ParticleTypes.LARGE_SMOKE, 20);
            }
        }
    }

    /** 主动瞬移：闪现至视线前方，或随机传送至范围内的暗处。 */
    public static class Teleport extends Ability {
        private final double range;
        private final boolean requireDarkTarget;

        public Teleport(String path, int cost, int cdSecs, double range, boolean requireDarkTarget) {
            super(GuimiMod.id(path), cost, cdSecs * 20, true);
            this.range = range;
            this.requireDarkTarget = requireDarkTarget;
        }

        @Override
        public void onActivate(Player player) {
            if (!(player instanceof ServerPlayer sp)) return;
            ServerLevel level = sp.serverLevel();
            Vec3 dest = null;
            if (requireDarkTarget) {
                // 随机采样落点，要求亮度 ≤ 3 且有立足之地
                RandomSource random = level.getRandom();
                for (int i = 0; i < 24 && dest == null; i++) {
                    BlockPos pos = sp.blockPosition().offset(
                            random.nextInt((int) range * 2 + 1) - (int) range,
                            random.nextInt(5) - 2,
                            random.nextInt((int) range * 2 + 1) - (int) range);
                    if (level.getMaxLocalRawBrightness(pos) <= 3
                            && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                            && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()
                            && !level.getBlockState(pos.below()).getCollisionShape(level, pos.below()).isEmpty()) {
                        dest = Vec3.atBottomCenterOf(pos);
                    }
                }
                if (dest == null) {
                    sp.displayClientMessage(
                            net.minecraft.network.chat.Component.translatable("message.guimi_mod.exp.no_shadow"), true);
                    ExpFx.refund(sp, getSpiritualityCost());
                    return;
                }
            } else {
                // 闪现：沿视线方向落到最远可站立位置
                Vec3 look = sp.getLookAngle();
                dest = sp.position();
                for (double d = range; d >= 2; d -= 1) {
                    Vec3 cand = sp.position().add(look.scale(d));
                    BlockPos pos = BlockPos.containing(cand);
                    if (level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                            && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()) {
                        dest = cand;
                        break;
                    }
                }
            }
            ExpFx.burst(level, sp, ParticleTypes.PORTAL, 30);
            sp.teleportTo(level, dest.x, dest.y, dest.z, sp.getYRot(), sp.getXRot());
            level.playSound(null, sp.blockPosition(),
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
            ExpFx.activated(sp, getNameKey());
        }
    }

    /** 主动冲锋：向前突进并对路径上的敌人造成伤害 + 击飞。 */
    public static class Dash extends Ability {
        private final double distance;
        private final float damage;

        public Dash(String path, int cost, int cdSecs, double distance, float damage) {
            super(GuimiMod.id(path), cost, cdSecs * 20, true);
            this.distance = distance;
            this.damage = damage;
        }

        @Override
        public void onActivate(Player player) {
            if (!(player instanceof ServerPlayer sp)) return;
            ServerLevel level = sp.serverLevel();
            Vec3 look = sp.getLookAngle();
            // 冲量推进（客户端同步由 hurtMarked 触发）
            sp.setDeltaMovement(look.x * distance / 4.0, Math.max(0.2, look.y * 0.4), look.z * distance / 4.0);
            sp.hurtMarked = true;
            for (LivingEntity e : ExpFx.rayTargets(sp, distance, 16)) {
                if (!ExpFx.Filter.HOSTILE.test(sp, e)) continue;
                e.hurt(sp.damageSources().playerAttack(sp), damage);
                e.setDeltaMovement(e.getDeltaMovement().add(0, 0.6, 0));
                e.hurtMarked = true;
                ExpFx.burst(level, e, ParticleTypes.CRIT, 15);
            }
            level.playSound(null, sp.blockPosition(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.8F);
            ExpFx.activated(sp, getNameKey());
        }
    }

    /** lambda 主动：无法归纳为通用模板的技能以内联逻辑实现。 */
    public static class Custom extends Ability {
        private final Consumer<ServerPlayer> action;

        public Custom(String path, int cost, int cdSecs, Consumer<ServerPlayer> action) {
            super(GuimiMod.id(path), cost, cdSecs * 20, true);
            this.action = action;
        }

        @Override
        public void onActivate(Player player) {
            if (player instanceof ServerPlayer sp) {
                action.accept(sp);
            }
        }
    }
}
