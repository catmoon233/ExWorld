package com.wan.gmmod.common.capability.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 反占卜灵性干扰场数据（世界级附件）：记录本维度所有干扰场的中心坐标、
 * 过期游戏刻与部署者的序列信息。
 * <p>
 * 任何占卜目标（玩家 / 生物 / 坐标）落在干扰场覆盖范围（水平 10×10、垂直 ±5）内时，
 * 占卜结果会被干扰（见 {@code AntiDivination}）。服务端权威，随存档持久化，无需同步。
 *
 * @param fields 干扰场列表
 */
public record InterferenceFieldData(List<Field> fields) {

    /** 干扰场覆盖半径（方块，水平 / 垂直） */
    public static final int RADIUS = 5;

    /**
     * 单个干扰场。
     *
     * @param pos      中心坐标（{@link BlockPos#asLong()} 打包）
     * @param expiry   过期游戏刻
     * @param ownerSeq 部署者序列等级（0 = 未就职）
     * @param witch    部署者是否为魔女途径（序列 4 以上可完全遮蔽半神以下的占卜）
     */
    public record Field(long pos, long expiry, int ownerSeq, boolean witch) {
        public static final Codec<Field> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.LONG.fieldOf("pos").forGetter(Field::pos),
                Codec.LONG.fieldOf("expiry").forGetter(Field::expiry),
                Codec.INT.fieldOf("owner_seq").forGetter(Field::ownerSeq),
                Codec.BOOL.fieldOf("witch").forGetter(Field::witch)
        ).apply(i, Field::new));

        /** 该干扰场是否覆盖指定坐标。 */
        public boolean covers(BlockPos target) {
            BlockPos center = BlockPos.of(pos);
            return Math.abs(target.getX() - center.getX()) <= RADIUS
                    && Math.abs(target.getY() - center.getY()) <= RADIUS
                    && Math.abs(target.getZ() - center.getZ()) <= RADIUS;
        }
    }

    public static final Codec<InterferenceFieldData> CODEC = Field.CODEC.listOf()
            .xmap(l -> new InterferenceFieldData(new ArrayList<>(l)), InterferenceFieldData::fields);

    public static InterferenceFieldData empty() {
        return new InterferenceFieldData(new ArrayList<>());
    }

    /** 部署一个干扰场。 */
    public void add(BlockPos center, long expiry, int ownerSeq, boolean witch) {
        fields.add(new Field(center.asLong(), expiry, ownerSeq, witch));
    }

    /** 移除所有已过期的干扰场。 */
    public void purgeExpired(long now) {
        fields.removeIf(f -> f.expiry() <= now);
    }

    /** 返回覆盖指定坐标且未过期的干扰场，没有则返回 {@code null}。 */
    @Nullable
    public Field covering(BlockPos target, long now) {
        for (Field f : fields) {
            if (f.expiry() > now && f.covers(target)) {
                return f;
            }
        }
        return null;
    }
}
