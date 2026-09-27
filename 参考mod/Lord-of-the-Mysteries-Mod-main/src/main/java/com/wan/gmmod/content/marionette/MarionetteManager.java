package com.wan.gmmod.content.marionette;

import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.common.network.packet.MarionetteActionPacket;
import com.wan.gmmod.common.network.packet.MarionetteControlInputPacket;
import com.wan.gmmod.common.network.packet.MarionetteViewPacket;
import com.wan.gmmod.common.network.packet.SpiritThreadSyncPacket;
import com.wan.gmmod.content.spirituality.SpiritualityManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 秘偶管理器（秘偶大师核心逻辑）。
 * <p>
 * 职责：
 * <ul>
 *   <li>灵体之线操控计时：操控满 5 秒后将目标「秘偶化」；</li>
 *   <li>秘偶维护：原始实体数据保留，仇恨永不指向主人，跟随 / 远距传送；</li>
 *   <li>秘偶战斗：主人潜行时自动索敌，代替主人战斗（使用秘偶原有能力）；</li>
 *   <li>右键命令：空手右键目标命令秘偶攻击，右键秘偶本身将其释放；</li>
 *   <li>共享视野：摄像机绑定秘偶、WASD 直接操控、左键近战 / 右键原有能力。</li>
 * </ul>
 * 每刻由 {@link com.wan.gmmod.common.event.GameEventSubscriber} 驱动。
 */
public final class MarionetteManager {
    /** 超过该距离时秘偶开始跟随主人（米） */
    private static final double FOLLOW_DIST = 4.0;
    /** 超过该距离时秘偶直接传送回主人身边（米） */
    private static final double TELEPORT_DIST = 30.0;
    /** 潜行代战时的自动索敌半径（米） */
    private static final double AUTO_COMBAT_RANGE = 16.0;
    /** 共享视野操控下的近战攻击距离（米） */
    private static final double CONTROL_MELEE_RANGE = 4.0;

    // ===== 掌控加深 / 彻底秘偶化 =====
    // 经过时间推移、掌控加深，秘偶大师可将目标彻底变成自己的秘偶：
    // 不再挣扎反噬、行动自然，可在更远距离内躲于幕后操纵其战斗（保留原有非凡能力）。
    /** 彻底秘偶化所需的累计掌控刻数（约 5 分钟；共享视野操控中 3 倍速累积） */
    private static final int THOROUGH_MASTERY_TICKS = 6000;
    /** 共享视野操控的灵体之线有效距离（米）：尚未彻底秘偶化 */
    private static final double CONTROL_RANGE = 24.0;
    /** 彻底秘偶化后的幕后操纵距离（米） */
    private static final double THOROUGH_CONTROL_RANGE = 64.0;

    // ===== 反抗 / 挣扎 / 反噬（灵体之线控制强度）=====
    // 灵体之线属强行控制：目标思维迟缓、身体僵硬、动作滞涩，几乎无法以能力对抗，
    // 只能依靠自身灵体强度摆脱——对序列5秘偶大师而言，半神之下几乎无碍。
    /** 挣扎判定间隔（刻） */
    private static final int STRUGGLE_INTERVAL = 20;
    /** 一次挣扎爆发的红线持续时长（刻） */
    private static final int STRUGGLE_BURST_TICKS = 30;
    /** 累计挣扎进度达到此值时秘偶挣脱操控 */
    private static final int BREAK_FREE_THRESHOLD = 100;
    /** 每次挣扎爆发消耗主人的灵性基数（按灵体强度缩放，弱小灵体几乎无消耗） */
    private static final int GRIP_DRAIN_PER_STRUGGLE = 2;
    /** 挣脱瞬间对主人的灵性反噬 */
    private static final int BACKLASH_SPIRITUALITY = 20;
    /** 半神层次的灵体强度阈值（以最大生命近似）：低于此值几乎不可能挣脱 */
    private static final float DEMIGOD_HEALTH = 100.0F;

    /** 每个玩家秘偶的累计挣扎进度（玩家 UUID → 进度），仅服务端 */
    private static final Map<UUID, Integer> STRUGGLE_PROGRESS = new ConcurrentHashMap<>();
    /** 挣扎爆发结束的游戏刻（玩家 UUID → 刻），期间红线抖动，仅服务端 */
    private static final Map<UUID, Long> STRUGGLING_UNTIL = new ConcurrentHashMap<>();

    /** 共享视野期间的最新操控输入（玩家 UUID → 输入包），仅服务端 */
    private static final Map<UUID, MarionetteControlInputPacket> CONTROL_INPUTS = new ConcurrentHashMap<>();

    private MarionetteManager() {}

    /** 每刻驱动：线程操控计时 + 秘偶维护（仅服务端）。 */
    public static void tickPlayer(ServerPlayer player) {
        ServerLevel level = player.serverLevel();

        // 1) 灵体之线操控计时：期满将目标秘偶化
        long controlEnd = player.getData(ModAttachments.THREAD_CONTROL_END);
        if (controlEnd > 0 && level.getGameTime() >= controlEnd) {
            player.setData(ModAttachments.THREAD_CONTROL_END, 0L);
            Mob target = findMob(level, player.getData(ModAttachments.THREAD_TARGET_UUID));
            player.setData(ModAttachments.THREAD_TARGET_UUID, "");
            if (target != null && target.isAlive()) {
                puppetize(player, target);
            }
        } else if (controlEnd > 0) {
            // 正在链接：向受牵引的目标展示一条灵体之线（目标挣扎反抗）
            Mob charging = findMob(level, player.getData(ModAttachments.THREAD_TARGET_UUID));
            if (charging != null && charging.isAlive() && player.tickCount % 10 == 0) {
                PacketDistributor.sendToPlayer(player,
                        new SpiritThreadSyncPacket(-1, charging.getId(), true));
            }
        }
        
        // 2) 秘偶维护
        String uuid = player.getData(ModAttachments.MARIONETTE_UUID);
        if (uuid.isEmpty()) {
            STRUGGLE_PROGRESS.remove(player.getUUID());
            STRUGGLING_UNTIL.remove(player.getUUID());
            return;
        }
        Mob marionette = findMob(level, uuid);
        if (marionette == null || !marionette.isAlive()) {
            player.setData(ModAttachments.MARIONETTE_UUID, "");
            player.setData(ModAttachments.MARIONETTE_MASTERY, 0);
            player.setData(ModAttachments.MARIONETTE_THOROUGH, false);
            stopControl(player);
            STRUGGLE_PROGRESS.remove(player.getUUID());
            STRUGGLING_UNTIL.remove(player.getUUID());
            return;
        }
        // 秘偶完全受控：永不攻击主人
        if (marionette.getTarget() == player) {
            marionette.setTarget(null);
        }
        if (marionette.getLastHurtByMob() == player) {
            marionette.setLastHurtByMob(null);
        }
        boolean controlling = player.getData(ModAttachments.MARIONETTE_CONTROLLING);
        // 掌控加深：随时间推移累积掌控度（操控中 3 倍速），期满彻底秘偶化
        boolean thorough = player.getData(ModAttachments.MARIONETTE_THOROUGH);
        if (!thorough) {
            int mastery = player.getData(ModAttachments.MARIONETTE_MASTERY) + (controlling ? 3 : 1);
            player.setData(ModAttachments.MARIONETTE_MASTERY, mastery);
            if (mastery >= THOROUGH_MASTERY_TICKS) {
                thorough = true;
                thoroughPuppetize(player, marionette);
            } else if (mastery == THOROUGH_MASTERY_TICKS / 2) {
                player.displayClientMessage(Component.translatable(
                        "ability.guimi_mod.marionette.mastery_deepening", marionette.getDisplayName()), true);
            }
        }
        // 受控标记：彻底秘偶化前如提线木偶般僵硬迟滞；彻底化后灵体完全顺服，行动自然且不再发光
        if (!thorough && player.tickCount % 40 == 0) {
            marionette.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, false, false));
            marionette.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0, false, false, false));
            marionette.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 60, 0, false, false, false));
        }
        // 反抗 / 挣扎 / 反噬：彻底秘偶再无挣脱可能；非操控状态下秘偶可能挣脱（挣脱则反噬主人并提前返回）
        if (!thorough && !controlling && tickStruggle(player, marionette)) {
            return;
        }
        // 同步灵体之线：主人 ↔ 秘偶（挣扎爆发期间红线抖动）
        if (player.tickCount % 10 == 0) {
            boolean struggling = level.getGameTime() < STRUGGLING_UNTIL.getOrDefault(player.getUUID(), 0L);
            PacketDistributor.sendToPlayer(player,
                    new SpiritThreadSyncPacket(marionette.getId(), -1, struggling));
        }
        // 共享视野操控中：由玩家输入直接驱动，跳过自动索敌 / 跟随
        if (controlling) {
            // 幕后操纵距离限制：灵体之线的有效长度（彻底秘偶化后大幅延长）
            double range = thorough ? THOROUGH_CONTROL_RANGE : CONTROL_RANGE;
            if (player.distanceTo(marionette) > range) {
                stopControl(player);
                player.displayClientMessage(Component.translatable(
                        "ability.guimi_mod.marionette.thread_limit", (int) range), true);
                return;
            }
            // 彻底秘偶：主人躲于幕后，本体隐身隐匿
            if (thorough && player.tickCount % 40 == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 0, false, false, false));
            }
            tickControl(player, marionette);
            return;
        }
        // 秘偶战斗：主人潜行时自动索敌、代替主人战斗
        if (player.isShiftKeyDown() && marionette.getTarget() == null && player.tickCount % 10 == 0) {
            level.getEntitiesOfClass(Monster.class,
                            marionette.getBoundingBox().inflate(AUTO_COMBAT_RANGE),
                            m -> m != marionette && m.isAlive())
                    .stream()
                    .min(Comparator.comparingDouble(marionette::distanceToSqr))
                    .ifPresent(marionette::setTarget);
        }
        // 跟随主人：无战斗目标时保持在身边
        if (marionette.getTarget() == null && player.tickCount % 10 == 0) {
            double distSqr = marionette.distanceToSqr(player);
            if (distSqr > TELEPORT_DIST * TELEPORT_DIST) {
                marionette.teleportTo(player.getX(), player.getY(), player.getZ());
            } else if (distSqr > FOLLOW_DIST * FOLLOW_DIST) {
                marionette.getNavigation().moveTo(player, 1.2);
            }
        }
    }

    /**
     * 反抗 / 挣扎 / 反噬：非操控（自主）状态下的秘偶会周期性尝试挣脱灵体之线束缚。
     * <p>
     * 灵体之线属强行控制，几乎无法以能力对抗，只能依靠自身灵体强度摆脱：
     * 每 {@link #STRUGGLE_INTERVAL} 刻判定一次挣扎（红线抖动、提示消息），但挣扎能否
     * 积累成挣脱取决于灵体强度（以最大生命近似，阈值 {@link #DEMIGOD_HEALTH}）：
     * <ul>
     *   <li>半神之下：挣扎进度增量微小且抵不过自然衰减，几乎永远无法挣脱（失败率极高）；</li>
     *   <li>半神层次（最大生命 ≥ 阈值）：挣扎真实有效，进度快速累积直至挣脱反噬。</li>
     * </ul>
     * 主人灵性被榨干时束缚同样崩溃（维持灵体之线需要灵性）。
     *
     * @return 秘偶是否已挣脱（挣脱后调用方应立即结束本刻的秘偶维护）
     */
    private static boolean tickStruggle(ServerPlayer player, Mob marionette) {
        UUID id = player.getUUID();
        ServerLevel level = player.serverLevel();
        // 非判定刻：让红线爆发自然结束，同时缓慢衰减挣扎进度
        if (player.tickCount % STRUGGLE_INTERVAL != 0) {
            return false;
        }
        int progress = STRUGGLE_PROGRESS.getOrDefault(id, 0);
        int spirituality = SpiritualityManager.get(player);
        int max = SpiritualityManager.getMax(player);
        // 束缚强度：主人灵性越充盈，掌控越稳固
        float gripRatio = max <= 0 ? 0.0F : Mth.clamp((float) spirituality / max, 0.0F, 1.0F);
        // 灵体强度：以最大生命近似，≥ 1.0 即半神层次
        float spiritBody = Mth.clamp(marionette.getMaxHealth() / DEMIGOD_HEALTH, 0.0F, 2.0F);
        boolean demigod = spiritBody >= 1.0F;
        // 挣扎尝试概率：强行控制下反抗难得且大多徒劳
        float chance = 0.10F + spiritBody * 0.20F + (1.0F - gripRatio) * 0.10F;
        if (player.getRandom().nextFloat() >= chance) {
            // 未挣扎：进度衰减，掌控趋于稳固
            if (progress > 0) {
                STRUGGLE_PROGRESS.put(id, Math.max(0, progress - 2));
            }
            return false;
        }
        // 挣扎爆发：红线抖动；进度增量由灵体强度决定——半神之下抵不过衰减，几乎不可能挣脱
        int gain = demigod ? 20 + (int) (spiritBody * 10.0F) : 1 + (int) (spiritBody * 3.0F);
        progress += gain;
        STRUGGLE_PROGRESS.put(id, progress);
        STRUGGLING_UNTIL.put(id, level.getGameTime() + STRUGGLE_BURST_TICKS);
        // 维持束缚的灵性代价按灵体强度缩放：弱小灵体的挣扎几乎不损耗灵性
        int drain = Math.round(GRIP_DRAIN_PER_STRUGGLE * spiritBody);
        int drained = Math.max(0, spirituality - drain);
        SpiritualityManager.set(player, drained);
        level.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                marionette.getX(), marionette.getY() + marionette.getBbHeight() + 0.2, marionette.getZ(),
                6, 0.25, 0.2, 0.25, 0.0);
        level.playSound(null, marionette.blockPosition(),
                SoundEvents.LEASH_KNOT_BREAK, SoundSource.PLAYERS, 0.7F, 0.6F);
        PacketDistributor.sendToPlayer(player,
                new SpiritThreadSyncPacket(marionette.getId(), -1, true));
        // 挣脱条件：进度封顶（仅半神层次现实可达）或主人灵性被榨干
        if (progress >= BREAK_FREE_THRESHOLD || drained <= 0) {
            breakFree(player, marionette);
            return true;
        }
        if (demigod) {
            player.displayClientMessage(Component.translatable(
                    "ability.guimi_mod.marionette.struggling", marionette.getDisplayName()), true);
        } else if (player.getRandom().nextFloat() < 0.3F) {
            // 半神之下：挣扎徒劳，低频提示避免刷屏
            player.displayClientMessage(Component.translatable(
                    "ability.guimi_mod.marionette.struggle_futile", marionette.getDisplayName()), true);
        }
        return false;
    }

    /**
     * 反噬：秘偶挣脱灵体之线，反噬秘偶大师本体。
     * <p>
     * 解除受控登记与共享视野，秘偶转为敌对并反攻主人；主人遭受灵性反噬并被施加
     * 虚弱 / 反胃 / 挖掘疲劳 / 失明等负面状态，播放诅咒音效并清除灵体之线。
     */
    private static void breakFree(ServerPlayer player, Mob marionette) {
        UUID id = player.getUUID();
        ServerLevel level = player.serverLevel();
        // 解除掌控
        stopControl(player);
        player.setData(ModAttachments.MARIONETTE_UUID, "");
        player.setData(ModAttachments.MARIONETTE_MASTERY, 0);
        player.setData(ModAttachments.MARIONETTE_THOROUGH, false);
        STRUGGLE_PROGRESS.remove(id);
        STRUGGLING_UNTIL.remove(id);
        // 秘偶挣脱：转为敌对，反攻昔日主人
        marionette.setPersistenceRequired();
        marionette.setTarget(player);
        marionette.setLastHurtByMob(player);
        // 灵性反噬
        int spirituality = SpiritualityManager.get(player);
        SpiritualityManager.set(player, Math.max(0, spirituality - BACKLASH_SPIRITUALITY));
        // 精神反噬：虚弱 / 反胃 / 挖掘疲劳 / 失明
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0));
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 0));
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 200, 1));
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
        level.playSound(null, player.blockPosition(),
                SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.PLAYERS, 1.0F, 0.8F);
        level.sendParticles(ParticleTypes.SOUL,
                player.getX(), player.getY() + player.getBbHeight() * 0.5, player.getZ(),
                24, 0.4, 0.6, 0.4, 0.02);
        // 清除灵体之线
        PacketDistributor.sendToPlayer(player, new SpiritThreadSyncPacket(-1, -1, false));
        player.displayClientMessage(Component.translatable(
                "ability.guimi_mod.marionette.broke_free", marionette.getDisplayName()), false);
    }

    /** 秘偶化：目标原始实体数据保留，仇恨清空、永不消失，登记为玩家的秘偶。 */
    public static void puppetize(ServerPlayer owner, Mob target) {
        target.setTarget(null);
        target.setLastHurtByMob(null);
        target.setPersistenceRequired();
        owner.setData(ModAttachments.MARIONETTE_UUID, target.getUUID().toString());
        // 新秘偶从零开始掌控：清除上一具秘偶累积的掌控度与彻底化标记
        owner.setData(ModAttachments.MARIONETTE_MASTERY, 0);
        owner.setData(ModAttachments.MARIONETTE_THOROUGH, false);
        STRUGGLE_PROGRESS.remove(owner.getUUID());
        STRUGGLING_UNTIL.remove(owner.getUUID());
        ServerLevel level = owner.serverLevel();
        level.sendParticles(ParticleTypes.END_ROD,
                target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                30, 0.3, 0.5, 0.3, 0.02);
        level.playSound(null, target.blockPosition(),
                SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.8F, 0.7F);
        owner.displayClientMessage(Component.translatable(
                "ability.guimi_mod.marionette.puppetized", target.getDisplayName()), false);
    }

    /** 获取玩家当前的秘偶，没有或已死亡返回 null（仅服务端）。 */
    public static Mob getMarionette(Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return null;
        }
        Mob mob = findMob(level, player.getData(ModAttachments.MARIONETTE_UUID));
        return mob != null && mob.isAlive() ? mob : null;
    }

    /** 释放秘偶：解除受控状态。 */
    public static void release(Player player, Mob marionette) {
        if (player instanceof ServerPlayer sp) {
            stopControl(sp);
        }
        player.setData(ModAttachments.MARIONETTE_UUID, "");
        player.setData(ModAttachments.MARIONETTE_MASTERY, 0);
        player.setData(ModAttachments.MARIONETTE_THOROUGH, false);
        marionette.setTarget(null);
        player.displayClientMessage(Component.translatable(
                "ability.guimi_mod.marionette.released", marionette.getDisplayName()), true);
    }

    /**
     * 彻底秘偶化：经过时间推移、掌控加深，目标彻底沦为秘偶大师的秘偶。
     * <p>
     * 灵体完全顺服：清除僵硬迟滞的受控标记、不再挣扎反噬；此后主人可在更远的
     * 距离内躲于幕后（本体隐身）操纵秘偶战斗，秘偶依旧可使用原有非凡能力。
     */
    private static void thoroughPuppetize(ServerPlayer player, Mob marionette) {
        player.setData(ModAttachments.MARIONETTE_THOROUGH, true);
        STRUGGLE_PROGRESS.remove(player.getUUID());
        STRUGGLING_UNTIL.remove(player.getUUID());
        // 解除提线木偶般的僵硬迟滞：行动恢复自然
        marionette.removeEffect(MobEffects.GLOWING);
        marionette.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        marionette.removeEffect(MobEffects.DIG_SLOWDOWN);
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                marionette.getX(), marionette.getY() + marionette.getBbHeight() * 0.5, marionette.getZ(),
                40, 0.3, 0.5, 0.3, 0.03);
        level.playSound(null, marionette.blockPosition(),
                SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.9F, 0.7F);
        player.displayClientMessage(Component.translatable(
                "ability.guimi_mod.marionette.thorough",
                marionette.getDisplayName(), (int) THOROUGH_CONTROL_RANGE), false);
    }

    // ==================== 共享视野（秘偶视角操控） ====================

    /** 切换「共享视野」：摄像机绑定秘偶，玩家本体挂机、WASD 操控秘偶。 */
    public static void toggleControl(ServerPlayer player) {
        if (player.getData(ModAttachments.MARIONETTE_CONTROLLING)) {
            stopControl(player);
            return;
        }
        Mob marionette = getMarionette(player);
        if (marionette == null) {
            player.displayClientMessage(
                    Component.translatable("ability.guimi_mod.shared_vision.no_marionette"), true);
            return;
        }
        player.setData(ModAttachments.MARIONETTE_CONTROLLING, true);
        PacketDistributor.sendToPlayer(player, new MarionetteViewPacket(true, marionette.getId()));
        player.displayClientMessage(Component.translatable("ability.guimi_mod.shared_vision.on"), true);
    }

    /** 退出「共享视野」：恢复摄像机与本体控制（幂等）。 */
    public static void stopControl(ServerPlayer player) {
        CONTROL_INPUTS.remove(player.getUUID());
        if (!player.getData(ModAttachments.MARIONETTE_CONTROLLING)) {
            return;
        }
        player.setData(ModAttachments.MARIONETTE_CONTROLLING, false);
        PacketDistributor.sendToPlayer(player, new MarionetteViewPacket(false, -1));
        player.displayClientMessage(Component.translatable("ability.guimi_mod.shared_vision.off"), true);
    }

    /** 登记客户端最新的操控输入（每刻由 {@link #tickControl} 消费）。 */
    public static void handleControlInput(ServerPlayer player, MarionetteControlInputPacket input) {
        if (player.getData(ModAttachments.MARIONETTE_CONTROLLING)) {
            CONTROL_INPUTS.put(player.getUUID(), input);
        }
    }

    /** 操控帧：视角同步 + WASD 移动 + 空格跳跃；保留 AI 但压制其导航与仇恨。 */
    private static void tickControl(ServerPlayer player, Mob marionette) {
        marionette.getNavigation().stop();
        marionette.setTarget(null);
        MarionetteControlInputPacket input = CONTROL_INPUTS.get(player.getUUID());
        if (input == null) {
            return;
        }
        // 视角同步：秘偶朝向完全跟随玩家操控视角
        marionette.setYRot(input.yRot());
        marionette.yRotO = input.yRot();
        marionette.yBodyRot = input.yRot();
        marionette.yHeadRot = input.yRot();
        marionette.setXRot(input.xRot());
        // 移动：按视角 yaw 把前后 / 左右输入转成世界方向，速度取秘偶自身移速属性
        float yawRad = input.yRot() * Mth.DEG_TO_RAD;
        double sin = Mth.sin(yawRad);
        double cos = Mth.cos(yawRad);
        Vec3 dir = new Vec3(input.strafe() * cos - input.forward() * sin, 0.0,
                input.forward() * cos + input.strafe() * sin);
        Vec3 dm = marionette.getDeltaMovement();
        if (dir.lengthSqr() > 1.0E-4) {
            double speed = marionette.getAttributeValue(Attributes.MOVEMENT_SPEED);
            dir = dir.normalize().scale(speed);
            marionette.setDeltaMovement(dir.x, dm.y, dir.z);
            marionette.hasImpulse = true;
        }
        if (input.jump() && marionette.onGround()) {
            marionette.setDeltaMovement(marionette.getDeltaMovement().x, 0.42,
                    marionette.getDeltaMovement().z);
            marionette.hasImpulse = true;
        }
    }

    /** 操控动作：退出 / 左键近战（秘偶基础攻击力）/ 右键触发原有非凡能力。 */
    public static void handleAction(ServerPlayer player, int action, int targetId) {
        if (!player.getData(ModAttachments.MARIONETTE_CONTROLLING)) {
            return;
        }
        if (action == MarionetteActionPacket.EXIT) {
            stopControl(player);
            return;
        }
        Mob marionette = getMarionette(player);
        if (marionette == null) {
            stopControl(player);
            return;
        }
        Entity target = targetId >= 0 ? player.serverLevel().getEntity(targetId) : null;
        if (action == MarionetteActionPacket.ATTACK) {
            // 近战攻击：挥臂 + 对拾取目标造成秘偶基础攻击力伤害
            marionette.swing(InteractionHand.MAIN_HAND, true);
            if (target instanceof LivingEntity living && living != player && living.isAlive()
                    && marionette.distanceTo(living) <= CONTROL_MELEE_RANGE) {
                marionette.doHurtTarget(living);
            }
        } else if (action == MarionetteActionPacket.ABILITY) {
            // 原有非凡能力：按秘偶种类映射（苦力怕自爆 / 末影人瞬移 / 烈焰人火球 / 远程攻击等）
            if (MarionetteAbilities.hasAbility(marionette)) {
                LivingEntity living = target instanceof LivingEntity l && l != player && l.isAlive() ? l : null;
                if (MarionetteAbilities.perform(player, marionette, living)) {
                    marionette.swing(InteractionHand.MAIN_HAND, true);
                } else {
                    player.displayClientMessage(
                            Component.translatable("ability.guimi_mod.shared_vision.no_target"), true);
                }
            } else {
                player.displayClientMessage(
                        Component.translatable("ability.guimi_mod.shared_vision.no_ability"), true);
            }
        }
    }

    /**
     * 空手右键命令：点击秘偶本身 → 释放；点击其他活体 → 命令秘偶攻击。
     *
     * @return 是否消费了这次交互
     */
    public static boolean handleCommand(Player player, Entity clicked) {
        Mob marionette = getMarionette(player);
        if (marionette == null) {
            return false;
        }
        if (clicked == marionette) {
            release(player, marionette);
            return true;
        }
        if (clicked instanceof LivingEntity living && living.isAlive()) {
            marionette.setTarget(living);
            player.displayClientMessage(Component.translatable(
                    "ability.guimi_mod.marionette_combat.attack", living.getDisplayName()), true);
            return true;
        }
        return false;
    }

    private static Mob findMob(ServerLevel level, String uuidStr) {
        if (uuidStr == null || uuidStr.isEmpty()) {
            return null;
        }
        try {
            return level.getEntity(UUID.fromString(uuidStr)) instanceof Mob mob ? mob : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
