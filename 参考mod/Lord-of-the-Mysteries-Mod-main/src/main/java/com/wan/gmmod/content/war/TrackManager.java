package com.wan.gmmod.content.war;

import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 痕迹追踪管理器——猎人（战争之红途径 · 序列 9）「痕迹追踪」的足迹记录。
 * <p>
 * 每秒记录一次已解锁玩家周围 32 米内生物的位置与朝向（全局环形缓冲，
 * 上限 2048 条，30 秒过期）。潜行右键地面时显现附近 10 米内的足迹粒子，
 * 并汇总 30 秒内经过的生物类型与去向。
 */
public final class TrackManager {

    /** 足迹有效期：30 秒。 */
    private static final long EXPIRE_TICKS = 600;
    /** 环形缓冲上限。 */
    private static final int MAX_RECORDS = 2048;
    /** 记录采样间隔（刻）。 */
    public static final int SAMPLE_INTERVAL = 20;
    /** 记录 / 显现半径。 */
    private static final double RECORD_RANGE = 32.0;
    private static final double REVEAL_RANGE = 10.0;

    /** 单条足迹：维度 + 位置 + 生物类型描述键 + 朝向 + 记录时刻。 */
    private record Footprint(ResourceKey<Level> dimension, Vec3 pos, String typeKey,
                             float yRot, long time) {
    }

    private static final Deque<Footprint> RECORDS = new ArrayDeque<>();

    private TrackManager() {
    }

    /** 每秒调用一次（玩家已解锁痕迹追踪）：记录周围生物的足迹。 */
    public static void record(ServerPlayer sp) {
        ServerLevel level = sp.serverLevel();
        long now = level.getGameTime();
        List<Mob> mobs = level.getEntitiesOfClass(Mob.class,
                sp.getBoundingBox().inflate(RECORD_RANGE), Mob::isAlive);
        for (Mob mob : mobs) {
            RECORDS.addLast(new Footprint(level.dimension(), mob.position(),
                    mob.getType().getDescriptionId(), mob.getYRot(), now));
        }
        // 清理：过期 + 超量
        while (!RECORDS.isEmpty() && (now - RECORDS.peekFirst().time() > EXPIRE_TICKS
                || RECORDS.size() > MAX_RECORDS)) {
            RECORDS.removeFirst();
        }
    }

    /** 潜行右键地面：显现附近 30 秒内的足迹（粒子 + 类型与方向汇总）。 */
    public static void reveal(ServerPlayer sp) {
        ServerLevel level = sp.serverLevel();
        long now = level.getGameTime();
        // 类型 → 最近一条足迹朝向（取最新的代表去向）
        Map<String, Float> summary = new LinkedHashMap<>();
        List<Footprint> nearby = new ArrayList<>();
        for (Footprint fp : RECORDS) {
            if (fp.dimension() == level.dimension()
                    && now - fp.time() <= EXPIRE_TICKS
                    && fp.pos().distanceTo(sp.position()) <= REVEAL_RANGE) {
                nearby.add(fp);
                summary.put(fp.typeKey(), fp.yRot());
            }
        }
        if (nearby.isEmpty()) {
            sp.displayClientMessage(Component.translatable("message.guimi_mod.track.none"), true);
            return;
        }
        // 足迹粒子：位置一缕灰尘 + 朝向偏移一粒火星指示去向
        for (Footprint fp : nearby) {
            Vec3 dir = Vec3.directionFromRotation(0, fp.yRot()).scale(0.4);
            level.sendParticles(ParticleTypes.GLOW, fp.pos().x, fp.pos().y + 0.1, fp.pos().z,
                    2, 0.05, 0.02, 0.05, 0);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    fp.pos().x + dir.x, fp.pos().y + 0.1, fp.pos().z + dir.z,
                    1, 0.02, 0.02, 0.02, 0);
        }
        // 汇总消息：生物类型 + 去向
        for (Map.Entry<String, Float> entry : summary.entrySet()) {
            Direction dir = Direction.fromYRot(entry.getValue());
            sp.sendSystemMessage(Component.translatable("message.guimi_mod.track.entry",
                    Component.translatable(entry.getKey()),
                    Component.translatable("message.guimi_mod.track.dir." + dir.getName())));
        }
    }
}
