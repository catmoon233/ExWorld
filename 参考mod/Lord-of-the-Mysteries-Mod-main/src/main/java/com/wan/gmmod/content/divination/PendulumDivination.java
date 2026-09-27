package com.wan.gmmod.content.divination;

import com.wan.gmmod.content.spirituality.SpiritualityManager;

import com.wan.gmmod.Config;
import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.common.network.packet.HighlightBlocksPacket;
import com.wan.gmmod.common.network.packet.PendulumUsePacket;
import com.wan.gmmod.content.sequences.Sequences;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 黄水晶灵摆占卜逻辑（全部在服务端执行）。
 * <p>
 * 统一入口 {@link #perform} 负责：
 * <ol>
 *     <li>校验前提条件（灵性、冷却、安全状态）；</li>
 *     <li>扣除灵性、开启冷却；</li>
 *     <li>依据目标类型做<b>加权随机</b>得出结果，并受序列等级 / 途径影响；</li>
 *     <li>执行结果效果（聊天反馈、状态效果、粒子、音效）；</li>
 *     <li>向追踪该玩家的客户端广播 {@link PendulumUsePacket} 驱动灵摆动画与抬臂。</li>
 * </ol>
 * 三类目标：{@link DivinationType#SELF 内省}、{@link DivinationType#ENTITY 探测}、
 * {@link DivinationType#POSITION 地理}。
 */
public final class PendulumDivination {
    /** 敌对生物侦测半径 */
    private static final double HOSTILE_RADIUS = 12.0;
    /** 地理占卜扫描半径（立方体） */
    private static final int SCAN_RADIUS = 16;
    /** 单次地理占卜最多标记的兴趣点数（防卡顿） */
    private static final int MAX_MARKS = 64;
    /** 方块高亮描边持续时间（tick，30 秒） */
    private static final int HIGHLIGHT_DURATION = 600;
    /** 长效状态时长（10 秒） */
    private static final int TICKS_10S = 200;
    /** 短效状态时长（5 秒） */
    private static final int TICKS_5S = 100;
    /** 战斗状态判定窗口（受伤后 5 秒） */
    private static final int COMBAT_WINDOW = 100;
    /** 灵性反噬扣除量 */
    private static final int BACKLASH_SPIRIT = 5;
    /** 昂贵清晰感知的额外灵性消耗（基础 10 + 5 = 15，即翻倍） */
    private static final int COSTLY_EXTRA_SPIRIT = 5;

    private PendulumDivination() {
    }

    // ---------------------------------------------------------------------
    // 结果枚举：权重（%）+ 对应灵摆摆动方向
    // ---------------------------------------------------------------------

    /** A. 内省占卜（针对自身） */
    public enum SelfOmen {
        REVELATION(30, PendulumSpin.CLOCKWISE),
        VAGUE(40, PendulumSpin.STILL),
        DANGER(20, PendulumSpin.CLOCKWISE),
        MISLEAD(10, PendulumSpin.COUNTERCLOCKWISE);

        final int weight;
        final PendulumSpin spin;

        SelfOmen(int weight, PendulumSpin spin) {
            this.weight = weight;
            this.spin = spin;
        }
    }

    /** B. 探测占卜（针对其他实体） */
    public enum EntityOmen {
        TRUE_INFO(40, PendulumSpin.CLOCKWISE),
        VAGUE_INFO(30, PendulumSpin.STILL),
        FALSE_INFO(20, PendulumSpin.COUNTERCLOCKWISE),
        BACKLASH(10, PendulumSpin.COUNTERCLOCKWISE);

        final int weight;
        final PendulumSpin spin;

        EntityOmen(int weight, PendulumSpin spin) {
            this.weight = weight;
            this.spin = spin;
        }
    }

    /** C. 地理占卜（针对位置） */
    public enum PositionOmen {
        CLEAR(35, PendulumSpin.CLOCKWISE),
        VAGUE(35, PendulumSpin.STILL),
        ERROR(20, PendulumSpin.COUNTERCLOCKWISE),
        COSTLY_CLEAR(10, PendulumSpin.CLOCKWISE);

        final int weight;
        final PendulumSpin spin;

        PositionOmen(int weight, PendulumSpin spin) {
            this.weight = weight;
            this.spin = spin;
        }
    }

    // ---------------------------------------------------------------------
    // 入口
    // ---------------------------------------------------------------------

    /**
     * 执行一次灵摆占卜（标准流程：自行校验前提、扣灵性、开启冷却）。
     *
     * @param player 占卜者（服务端）
     * @param type   目标类型
     * @param target 目标实体（{@link DivinationType#ENTITY} 时有效，可为 null）
     * @param pos    目标方块（{@link DivinationType#POSITION} 时有效，可为 null）
     * @return 是否成功发起占卜（前提不满足则返回 false）
     */
    public static boolean perform(ServerPlayer player, DivinationType type,
                                  @Nullable LivingEntity target, @Nullable BlockPos pos) {
        return perform(player, type, target, pos, 0, false);
    }

    /**
     * 执行一次占卜（可定制版，供魔镜占卜复用）。
     *
     * @param truthBonus 真实 / 清晰结果的额外权重（魔镜占卜 +20）
     * @param skipChecks 跳过前提校验与灵性 / 冷却扣除（调用方已自行处理时传 true）
     */
    public static boolean perform(ServerPlayer player, DivinationType type,
                                  @Nullable LivingEntity target, @Nullable BlockPos pos,
                                  int truthBonus, boolean skipChecks) {
        ServerLevel level = player.serverLevel();

        if (!skipChecks) {
            // 1. 前提校验
            Component fail = checkPrerequisites(player, level);
            if (fail != null) {
                player.sendSystemMessage(fail);
                return false;
            }

            // 2. 扣除灵性 + 开启冷却
            addSpirituality(player, -Config.DIVINATION_SPIRITUALITY_COST.getAsInt());
            long cooldownEnd = level.getGameTime() + Config.DIVINATION_COOLDOWN_SECONDS.getAsInt() * 20L;
            player.setData(ModAttachments.DIVINATION_COOLDOWN_END, cooldownEnd);
        }

        // 3. 按目标类型求结果并执行效果
        PendulumSpin spin = switch (type) {
            case ENTITY -> divineEntity(player, level, target, truthBonus);
            case POSITION -> divinePosition(player, level, pos, truthBonus);
            default -> divineSelf(player, level, truthBonus);
        };

        // 4. 音效 + 5. 广播动画（含抬臂）
        level.playSound(null, player.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.0F);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                player, new PendulumUsePacket(player.getId(), spin.ordinal()));
        return true;
    }

    // ---------------------------------------------------------------------
    // 前提校验
    // ---------------------------------------------------------------------

    @Nullable
    private static Component checkPrerequisites(ServerPlayer player, ServerLevel level) {
        int cost = Config.DIVINATION_SPIRITUALITY_COST.getAsInt();
        if (SpiritualityManager.get(player) < cost) {
            return Component.translatable("divination.guimi_mod.fail.spirituality", cost);
        }
        long now = level.getGameTime();
        long cooldownEnd = player.getData(ModAttachments.DIVINATION_COOLDOWN_END);
        if (now < cooldownEnd) {
            return Component.translatable("divination.guimi_mod.fail.cooldown", (cooldownEnd - now) / 20 + 1);
        }
        if (Config.DIVINATION_REQUIRE_SAFE_STATE.get()) {
            long lastDamage = player.getData(ModAttachments.LAST_DAMAGE_TICK);
            boolean inCombat = lastDamage > 0 && now - lastDamage < COMBAT_WINDOW;
            if (player.isInWater() || player.isPassenger() || inCombat) {
                return Component.translatable("divination.guimi_mod.fail.unsafe");
            }
        }
        return null;
    }

    // ---------------------------------------------------------------------
    // A. 内省占卜
    // ---------------------------------------------------------------------

    private static PendulumSpin divineSelf(ServerPlayer player, ServerLevel level, int truthBonus) {
        Map<SelfOmen, Integer> weights = new EnumMap<>(SelfOmen.class);
        for (SelfOmen o : SelfOmen.values()) {
            weights.put(o, o.weight);
        }
        weights.computeIfPresent(SelfOmen.REVELATION, (k, v) -> v + truthBonus);
        applyPathwayExtremes(player, weights, SelfOmen.REVELATION, SelfOmen.MISLEAD);
        SelfOmen omen = pick(level.random, weights);

        switch (omen) {
            case REVELATION -> {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, TICKS_10S, 0));
                player.sendSystemMessage(Component.translatable("divination.guimi_mod.self.revelation"));
                addActingProgress(player, 1);
            }
            case VAGUE -> player.sendSystemMessage(Component.translatable("divination.guimi_mod.self.vague"));
            case DANGER -> {
                if (hasHostileNearby(level, player)) {
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, TICKS_5S, 0));
                    player.sendSystemMessage(Component.translatable("divination.guimi_mod.self.danger"));
                } else {
                    player.sendSystemMessage(Component.translatable("divination.guimi_mod.self.danger_safe"));
                }
            }
            case MISLEAD -> {
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, TICKS_5S, 0));
                player.sendSystemMessage(Component.translatable("divination.guimi_mod.self.mislead"));
            }
        }
        return omen.spin;
    }

    // ---------------------------------------------------------------------
    // B. 探测占卜
    // ---------------------------------------------------------------------

    private static PendulumSpin divineEntity(ServerPlayer player, ServerLevel level,
                                             @Nullable LivingEntity target, int truthBonus) {
        if (target == null) {
            return divineSelf(player, level, truthBonus);
        }
        Map<EntityOmen, Integer> weights = new EnumMap<>(EntityOmen.class);
        for (EntityOmen o : EntityOmen.values()) {
            weights.put(o, o.weight);
        }
        weights.computeIfPresent(EntityOmen.TRUE_INFO, (k, v) -> v + truthBonus);
        // 序列越高（占卜家序列 5 以上）→ 真实信息更高、反噬更低
        applySequenceBoost(player, weights, EntityOmen.TRUE_INFO, EntityOmen.BACKLASH);
        // 命运之轮（怪物）途径 → 结果趋于极端
        applyPathwayExtremes(player, weights, EntityOmen.TRUE_INFO, EntityOmen.BACKLASH);
        // 反占卜干扰：目标受保护时真实信息归零（减弱时恢复 30%）
        AntiDivination.Interference interference = AntiDivination.check(player, target, null);
        if (interference != AntiDivination.Interference.NONE) {
            weights.put(EntityOmen.TRUE_INFO,
                    interference == AntiDivination.Interference.WEAKENED ? 30 : 0);
            player.sendSystemMessage(Component.translatable("divination.guimi_mod.interfered"));
        }
        EntityOmen omen = pick(level.random, weights);

        switch (omen) {
            case TRUE_INFO -> {
                int hp = (int) Math.ceil(target.getHealth());
                int maxHp = (int) Math.ceil(target.getMaxHealth());
                Component seqInfo;
                if (target instanceof ServerPlayer targetPlayer) {
                    int lvl = targetPlayer.getData(ModAttachments.SEQUENCE_LEVEL);
                    seqInfo = lvl > 0
                            ? Component.translatable("divination.guimi_mod.entity.seq", lvl)
                            : Component.translatable("divination.guimi_mod.entity.no_seq");
                } else {
                    seqInfo = Component.translatable("divination.guimi_mod.entity.no_seq");
                }
                player.sendSystemMessage(Component.translatable("divination.guimi_mod.entity.true_info",
                        target.getDisplayName(), hp, maxHp, seqInfo));
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, TICKS_5S, 0));
                addActingProgress(player, 2);
            }
            case VAGUE_INFO -> player.sendSystemMessage(Component.translatable(
                    "divination.guimi_mod.entity.vague", directionComponent(player, target.position())));
            case FALSE_INFO -> {
                int i = level.random.nextInt(3) + 1;
                player.sendSystemMessage(Component.translatable("divination.guimi_mod.entity.false_" + i));
            }
            case BACKLASH -> {
                addSpirituality(player, -BACKLASH_SPIRIT);
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
                player.sendSystemMessage(Component.translatable("divination.guimi_mod.entity.backlash"));
            }
        }
        return omen.spin;
    }

    // ---------------------------------------------------------------------
    // C. 地理占卜
    // ---------------------------------------------------------------------

    private static PendulumSpin divinePosition(ServerPlayer player, ServerLevel level,
                                               @Nullable BlockPos pos, int truthBonus) {
        BlockPos center = pos != null ? pos : player.blockPosition();
        Map<PositionOmen, Integer> weights = new EnumMap<>(PositionOmen.class);
        for (PositionOmen o : PositionOmen.values()) {
            weights.put(o, o.weight);
        }
        weights.computeIfPresent(PositionOmen.CLEAR, (k, v) -> v + truthBonus);
        applyPathwayExtremes(player, weights, PositionOmen.CLEAR, PositionOmen.ERROR);
        // 反占卜干扰：目标坐标被干扰场覆盖时清晰结果归零（减弱时恢复 30%）
        AntiDivination.Interference interference = AntiDivination.check(player, null, center);
        if (interference != AntiDivination.Interference.NONE) {
            weights.put(PositionOmen.CLEAR,
                    interference == AntiDivination.Interference.WEAKENED ? 30 : 0);
            weights.put(PositionOmen.COSTLY_CLEAR, 0);
            player.sendSystemMessage(Component.translatable("divination.guimi_mod.interfered"));
        }
        PositionOmen omen = pick(level.random, weights);

        switch (omen) {
            case CLEAR -> {
                int marked = markPointsOfInterest(player, level, center);
                if (marked > 0) {
                    player.sendSystemMessage(Component.translatable("divination.guimi_mod.pos.clear", marked));
                    addActingProgress(player, 1);
                } else {
                    player.sendSystemMessage(Component.translatable("divination.guimi_mod.pos.clear_none"));
                }
            }
            case VAGUE -> {
                int i = level.random.nextInt(2) + 1;
                player.sendSystemMessage(Component.translatable("divination.guimi_mod.pos.vague_" + i));
            }
            case ERROR -> player.sendSystemMessage(Component.translatable("divination.guimi_mod.pos.error"));
            case COSTLY_CLEAR -> {
                addSpirituality(player, -COSTLY_EXTRA_SPIRIT);
                int marked = markPointsOfInterest(player, level, center);
                player.sendSystemMessage(Component.translatable("divination.guimi_mod.pos.costly", marked));
            }
        }
        return omen.spin;
    }

    // ---------------------------------------------------------------------
    // 影响因素与工具方法
    // ---------------------------------------------------------------------

    /** 序列越高（1~5 序列）→ 提高 boost 权重、降低 reduce 权重 */
    private static <T> void applySequenceBoost(ServerPlayer player, Map<T, Integer> weights, T boost, T reduce) {
        int lvl = player.getData(ModAttachments.SEQUENCE_LEVEL);
        if (lvl >= 1 && lvl <= 5) {
            weights.computeIfPresent(boost, (k, v) -> v + 20);
            weights.computeIfPresent(reduce, (k, v) -> Math.max(0, v - 8));
        }
    }

    /** 命运之轮（怪物）途径 → 极端结果（best/worst）权重提高，其余降低 */
    private static <T> void applyPathwayExtremes(ServerPlayer player, Map<T, Integer> weights, T best, T worst) {
        String key = player.getData(ModAttachments.PATHWAY);
        if (!Sequences.Pathway.WHEEL.getKey().equals(key)) {
            return;
        }
        weights.computeIfPresent(best, (k, v) -> v + 15);
        weights.computeIfPresent(worst, (k, v) -> v + 15);
        for (Map.Entry<T, Integer> e : weights.entrySet()) {
            if (!e.getKey().equals(best) && !e.getKey().equals(worst)) {
                e.setValue(Math.max(0, e.getValue() - 10));
            }
        }
    }

    /** 加权随机挑选 */
    private static <T> T pick(RandomSource random, Map<T, Integer> weights) {
        int total = 0;
        for (int v : weights.values()) {
            total += Math.max(0, v);
        }
        if (total <= 0) {
            return weights.keySet().iterator().next();
        }
        int r = random.nextInt(total);
        for (Map.Entry<T, Integer> e : weights.entrySet()) {
            r -= Math.max(0, e.getValue());
            if (r < 0) {
                return e.getKey();
            }
        }
        return weights.keySet().iterator().next();
    }

    private static void addSpirituality(ServerPlayer player, int delta) {
        int v = SpiritualityManager.get(player);
        int max = com.wan.gmmod.content.spirituality.SpiritualityManager.getMax(player);
        SpiritualityManager.set(player,
                Math.max(0, Math.min(max, v + delta)));
    }

    private static void addActingProgress(ServerPlayer player, int amount) {
        int progress = player.getData(ModAttachments.ACTING_PROGRESS);
        if (progress < 100) {
            player.setData(ModAttachments.ACTING_PROGRESS, Math.min(100, progress + amount));
        }
    }

    private static boolean hasHostileNearby(ServerLevel level, ServerPlayer player) {
        AABB area = player.getBoundingBox().inflate(HOSTILE_RADIUS);
        return !level.getEntitiesOfClass(Monster.class, area).isEmpty();
    }

    /** 计算目标相对玩家的八向方位，返回对应翻译组件（供通灵线索复用） */
    public static Component directionComponent(ServerPlayer player, Vec3 targetPos) {
        double dx = targetPos.x - player.getX();
        double dz = targetPos.z - player.getZ();
        // 0° = 正北(-Z)，顺时针增大：东=90°，南=180°，西=270°
        double deg = (Math.toDegrees(Math.atan2(dx, -dz)) + 360.0) % 360.0;
        String[] keys = {"n", "ne", "e", "se", "s", "sw", "w", "nw"};
        int index = (int) Math.round(deg / 45.0) % 8;
        return Component.translatable("divination.guimi_mod.dir." + keys[index]);
    }

    /**
     * 扫描周围矿石 / 宝箱并收集坐标，通过 {@link HighlightBlocksPacket}
     * 发送给玩家，由客户端渲染层绘制持续描边线框，返回标记数量。
     */
    private static int markPointsOfInterest(ServerPlayer player, ServerLevel level, BlockPos center) {
        List<BlockPos> found = new ArrayList<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = -SCAN_RADIUS; x <= SCAN_RADIUS && found.size() < MAX_MARKS; x++) {
            for (int y = -SCAN_RADIUS; y <= SCAN_RADIUS && found.size() < MAX_MARKS; y++) {
                for (int z = -SCAN_RADIUS; z <= SCAN_RADIUS && found.size() < MAX_MARKS; z++) {
                    pos.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    BlockState state = level.getBlockState(pos);
                    if (state.is(Tags.Blocks.ORES) || state.getBlock() instanceof ChestBlock) {
                        found.add(pos.immutable());
                    }
                }
            }
        }
        if (!found.isEmpty()) {
            PacketDistributor.sendToPlayer(player, new HighlightBlocksPacket(found, HIGHLIGHT_DURATION));
        }
        return found.size();
    }
}
