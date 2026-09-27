package com.wan.gmmod.common.capability.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 扭曲区域数据（世界级附件）：记录本维度所有扭曲场（封闭屏障 / 隔绝房间）的中心、
 * 尺寸、过期游戏刻、屏障耐久与施法者。
 * <p>
 * 两种类型的扭曲区域占用同一张表，以 {@link Ring#type()} 区分：
 * <ul>
 *   <li>{@code TYPE_SEAL}（封闭屏障）：扭曲「关门」产生。以门方块为中心的水平矩形
 *   屏障，阻挡实体穿越，反复左键攻击边界方块可削减屏障耐久（耐久耗尽即破碎）；</li>
 *   <li>{@code TYPE_ISOLATE}（隔绝房间）：扭曲「不要打扰」产生。矩形空间内外隔离，
 *   非施法者 / 非高序列者无法踏入，踏入者被强制退出。</li>
 * </ul>
 * 服务端权威，随存档持久化，无需同步；边界视觉由客户端 {@code DistortionZoneSyncPacket}
 * 下发边界方块后自行描边。
 *
 * @param rings 扭曲区域列表
 */
public record DistortionZoneData(List<Ring> rings) {

    /** 扭曲区域类型：封闭屏障 */
    public static final int TYPE_SEAL = 0;
    /** 扭曲区域类型：隔绝房间 */
    public static final int TYPE_ISOLATE = 1;

    public static final Codec<DistortionZoneData> CODEC = Ring.CODEC.listOf()
            .xmap(l -> new DistortionZoneData(new ArrayList<>(l)), DistortionZoneData::rings);

    public static DistortionZoneData empty() {
        return new DistortionZoneData(new ArrayList<>());
    }

    /**
     * 单个扭曲区域。
     *
     * @param min      最小角坐标（{@link BlockPos#asLong()} 打包）
     * @param max      最大角坐标（{@link BlockPos#asLong()} 打包）
     * @param type     区域类型（{@link #TYPE_SEAL} / {@link #TYPE_ISOLATE}）
     * @param expiry   过期游戏刻
     * @param maxHp    屏障最大耐久（基于施法者灵性）
     * @param hp       当前耐久
     * @param owner    施法者 UUID 字符串
     */
    public record Ring(long min, long max, int type, long expiry,
                       int maxHp, int hp, String owner) {
        public static final Codec<Ring> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.LONG.fieldOf("min").forGetter(Ring::min),
                Codec.LONG.fieldOf("max").forGetter(Ring::max),
                Codec.INT.fieldOf("type").forGetter(Ring::type),
                Codec.LONG.fieldOf("expiry").forGetter(Ring::expiry),
                Codec.INT.fieldOf("max_hp").forGetter(Ring::maxHp),
                Codec.INT.fieldOf("hp").forGetter(Ring::hp),
                Codec.STRING.fieldOf("owner").forGetter(Ring::owner)
        ).apply(i, Ring::new));

        public BlockPos minPos() {
            return BlockPos.of(min);
        }

        public BlockPos maxPos() {
            return BlockPos.of(max);
        }

        /** 该环覆盖的方块坐标（AABB 包围盒）。 */
        public boolean covers(BlockPos pos) {
            BlockPos a = minPos();
            BlockPos b = maxPos();
            return pos.getX() >= a.getX() && pos.getX() <= b.getX()
                    && pos.getY() >= a.getY() && pos.getY() <= b.getY()
                    && pos.getZ() >= a.getZ() && pos.getZ() <= b.getZ();
        }

        /** 该环中心坐标（用于特效定位）。
         *
         * @return 中心 {@link BlockPos}
         */
        public BlockPos center() {
            BlockPos a = minPos();
            BlockPos b = maxPos();
            return new BlockPos((a.getX() + b.getX()) / 2, (a.getY() + b.getY()) / 2,
                    (a.getZ() + b.getZ()) / 2);
        }

        /** 减伤一次屏障耐久；返回扣除后是否仍有耐久。 */
        public boolean damage(int amount) {
            return hp - amount > 0;
        }

        /** 用当前耐久生成新实例（持久化扣血）。 */
        public Ring withHp(int newHp) {
            return new Ring(min, max, type, expiry, maxHp, Math.max(0, newHp), owner);
        }
    }

    /** 部署一个扭曲区域。 */
    public void addRing(BlockPos min, BlockPos max, int type, long expiry, int maxHp, String owner) {
        rings.add(new Ring(min.asLong(), max.asLong(), type, expiry, maxHp, maxHp, owner));
    }

    /** 移除所有已过期的扭曲区域。 */
    public void purgeExpired(long now) {
        rings.removeIf(r -> r.expiry() <= now);
    }

    /** 返回覆盖指定坐标且未过期的扭曲区域，没有则返回 {@code null}。 */
    @Nullable
    public Ring covering(BlockPos pos, long now) {
        for (Ring r : rings) {
            if (r.expiry() > now && r.covers(pos)) {
                return r;
            }
        }
        return null;
    }
}