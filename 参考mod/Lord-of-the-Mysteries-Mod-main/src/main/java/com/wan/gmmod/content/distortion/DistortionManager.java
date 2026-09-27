package com.wan.gmmod.content.distortion;

import com.wan.gmmod.content.spirituality.SpiritualityManager;

import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.common.capability.data.DistortionZoneData;
import com.wan.gmmod.common.network.packet.DistortionModeSyncPacket;
import com.wan.gmmod.common.network.packet.DistortionZoneSyncPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 扭曲能力核心（服务端权威）。
 * <p>
 * 扭曲是「黑皇帝 · 序列6 腐化男爵」的规则级操控能力，改变动作 / 意图 / 规则的含义而非
 * 直接造成伤害；「门 · 序列6 记录官」与「命运之轮 · 序列6 灾祸教士」拥有弱化版。
 * <p>
 * 五种扭曲效果：
 * <ul>
 *   <li><b>移动反向</b> {@code TYPE_MOVE_INVERT}：目标的移动向量被反向（持续数秒）；</li>
 *   <li><b>攻击转移</b> {@code TYPE_ATTACK_REDIRECT}：攻击者攻击施法者的攻击被取消，
 *   攻击目标被强制改为附近其他生物；</li>
 *   <li><b>弹射物偏转</b> {@code TYPE_PROJECTILE_DEFLECT}：施法者开启偏转窗口，
 *   进入 10 格内的弹射物每 tick 被施加向上 / 反向偏移；</li>
 *   <li><b>占卜劫持</b> {@code TYPE_DIVINATION_HIJACK}：目标玩家进行魔镜占卜 / 通灵时，
 *   真实结果被截获发给施法者，原提问者收到误导信息；</li>
 *   <li><b>区域扭曲</b>：扭曲「关门」封闭屏障 / 扭曲「不要打扰」隔绝房间
 *   （存储于世界级 {@link DistortionZoneData}，可被攻击击破）。</li>
 * </ul>
 * 实体级标记存储在目标的 {@code persistentData}（临时、不随存档长期保留），区域存储于
 * Level 附件。到期 / 破碎由 {@link DistortionEventSubscriber} 周期清理。
 */
public final class DistortionManager {

    /** 实体扭曲标记的 NBT 键（整个 CompoundTag 挂在 persistentData 下） */
    private static final String DIST_TAG = "gmmod_distortion";
    /** 扭曲类型 */
    private static final String DIST_TYPE = "type";
    /** 到期游戏刻 */
    private static final String DIST_UNTIL = "until";
    /** 施法者 UUID 字符串 */
    private static final String DIST_OWNER = "owner";

    /** 施法者「扭曲模式」窗口结束游戏刻（0 = 未开启） */
    private static final String MODE_UNTIL = "gmmod_distort_mode_until";
    /** 施法者「弹射物偏转」窗口结束游戏刻（0 = 未开启） */
    private static final String DEFLECT_UNTIL = "gmmod_deflect_until";

    // ---- 扭曲类型 ----
    /** 移动反向 */
    public static final String TYPE_MOVE_INVERT = "move_invert";
    /** 攻击转移 */
    public static final String TYPE_ATTACK_REDIRECT = "attack_redirect";
    /** 占卜 / 通灵劫持 */
    public static final String TYPE_DIVINATION_HIJACK = "divination_hijack";
    /** 弹射物偏转（挂在施法者身上） */
    public static final String TYPE_PROJECTILE_DEFLECT = "projectile_deflect";

    // ---- 区域常量 ----
    /** 封闭屏障（扭曲「关门」）时长：20 秒 */
    public static final int SEAL_DURATION_TICKS = 20 * 20;
    /** 隔绝房间（扭曲「不要打扰」）时长：20 秒 */
    public static final int ISOLATE_DURATION_TICKS = 20 * 20;
    /** 封闭屏障水平半径（方块） */
    public static final int SEAL_RADIUS = 5;
    /** 封闭屏障垂直半高（方块） */
    public static final int SEAL_HEIGHT = 4;
    /** 隔绝房间边长（方块） */
    public static final int ISOLATE_SIZE = 10;
    /** 弹射物偏转半径（方块） */
    public static final double DEFLECT_RANGE = 10.0;
    /** 弹射物偏转施加的向上增量（每 tick） */
    public static final double DEFLECT_UPWARD = 0.35;
    /** 弹射物偏转施加的横向扰动 */
    public static final double DEFLECT_SWERVE = 0.10;

    private DistortionManager() {
    }

    // =====================================================================
    // 扭曲模式（施法窗口）
    // =====================================================================

    /** 进入扭曲模式：记录窗口（15 秒）。重复调用刷新窗口。 */
    public static void enterMode(ServerPlayer sp) {
        sp.getPersistentData().putLong(MODE_UNTIL, sp.serverLevel().getGameTime() + 15 * 20L);
        PacketDistributor.sendToPlayer(sp, new DistortionModeSyncPacket(true));
    }

    /** 当前是否处于扭曲模式。 */
    public static boolean isInMode(ServerPlayer sp) {
        long until = sp.getPersistentData().getLong(MODE_UNTIL);
        return until > sp.serverLevel().getGameTime();
    }

    /** 退出扭曲模式。 */
    public static void exitMode(ServerPlayer sp) {
        sp.getPersistentData().remove(MODE_UNTIL);
        PacketDistributor.sendToPlayer(sp, new DistortionModeSyncPacket(false));
    }

    // =====================================================================
    // 实体级扭曲标记
    // =====================================================================

    /** 对目标施加一种实体扭曲标记（持续 seconds 秒）。 */
    public static void applyEntityDistortion(ServerPlayer owner, LivingEntity target,
                                             String type, int seconds) {
        if (target == owner) {
            return;
        }
        CompoundTag tag = target.getPersistentData().getCompound(DIST_TAG);
        tag.putString(DIST_TYPE, type);
        tag.putLong(DIST_UNTIL, owner.serverLevel().getGameTime() + seconds * 20L);
        tag.putString(DIST_OWNER, owner.getUUID().toString());
        target.getPersistentData().put(DIST_TAG, tag);
    }

    /** 目标身上是否存在未过期的指定扭曲标记。 */
    public static boolean hasDistortion(LivingEntity target, String type) {
        CompoundTag tag = target.getPersistentData().getCompound(DIST_TAG);
        if (!type.equals(tag.getString(DIST_TYPE))) {
            return false;
        }
        if (!target.level().isClientSide) {
            long until = tag.getLong(DIST_UNTIL);
            if (until > 0 && until <= target.level().getGameTime()) {
                removeDistortion(target);
                return false;
            }
        }
        return !tag.isEmpty();
    }

    /** 目标身上任意未过期扭曲的施法者 UUID，没有返回 {@code null}。 */
    public static UUID distortionOwner(LivingEntity target) {
        CompoundTag tag = target.getPersistentData().getCompound(DIST_TAG);
        if (tag.isEmpty()) {
            return null;
        }
        String owner = tag.getString(DIST_OWNER);
        return owner.isEmpty() ? null : UUID.fromString(owner);
    }

    /** 移除目标身上的扭曲标记。 */
    public static void removeDistortion(LivingEntity target) {
        target.getPersistentData().remove(DIST_TAG);
    }

    // =====================================================================
    // 弹射物偏转窗口（B）
    // =====================================================================

    /** 开启弹射物偏转窗口（持续 seconds 秒）。 */
    public static void startDeflect(ServerPlayer sp, int seconds) {
        sp.getPersistentData().putLong(DEFLECT_UNTIL,
                sp.serverLevel().getGameTime() + seconds * 20L);
        sp.getPersistentData().putString(DIST_TYPE, TYPE_PROJECTILE_DEFLECT); // 供查询
        sp.getPersistentData().putLong(DIST_UNTIL, sp.serverLevel().getGameTime() + seconds * 20L);
        sp.getPersistentData().putString(DIST_OWNER, sp.getUUID().toString());
    }

    /** 施法者当前是否开启弹射物偏转窗口。 */
    public static boolean isDeflecting(ServerPlayer sp) {
        long until = sp.getPersistentData().getLong(DEFLECT_UNTIL);
        if (until <= sp.serverLevel().getGameTime()) {
            sp.getPersistentData().remove(DEFLECT_UNTIL);
            return false;
        }
        return true;
    }

    /** 清空弹射物偏转窗口。 */
    public static void stopDeflect(ServerPlayer sp) {
        sp.getPersistentData().remove(DEFLECT_UNTIL);
    }

    // =====================================================================
    // 区域扭曲（A 封闭屏障 / C 隔绝房间）
    // =====================================================================

    /**
     * 扭曲「关门」：以门方块为中心生成封闭屏障（水平 ±{@link #SEAL_RADIUS}、
     * 垂直 ±{@link #SEAL_HEIGHT}），持续 {@link #SEAL_DURATION_TICKS}。
     * 屏障血量基于施法者灵性（20 + 灵性/5）。
     */
    public static void sealDoor(ServerPlayer sp, BlockPos doorPos) {
        ServerLevel level = sp.serverLevel();
        BlockPos min = doorPos.offset(-SEAL_RADIUS, -SEAL_HEIGHT, -SEAL_RADIUS);
        BlockPos max = doorPos.offset(SEAL_RADIUS, SEAL_HEIGHT, SEAL_RADIUS);
        int hp = 20 + SpiritualityManager.get(sp) / 5;
        registerRing(level, min, max, DistortionZoneData.TYPE_SEAL,
                level.getGameTime() + SEAL_DURATION_TICKS, hp, sp.getUUID());
    }

    /** 扭曲「不要打扰」：在 min~max 矩形空间内创建隔绝房间（聊天除外，无法与外部交互）。 */
    public static void isolateRegion(ServerPlayer sp, BlockPos min, BlockPos max) {
        ServerLevel level = sp.serverLevel();
        int hp = 20 + SpiritualityManager.get(sp) / 5;
        registerRing(level, min, max, DistortionZoneData.TYPE_ISOLATE,
                level.getGameTime() + ISOLATE_DURATION_TICKS, hp, sp.getUUID());
    }

    private static void registerRing(ServerLevel level, BlockPos min, BlockPos max,
                                     int type, long expiry, int hp, UUID owner) {
        DistortionZoneData data = level.getData(ModAttachments.DISTORTION_ZONES);
        data.addRing(min, max, type, expiry, hp, owner.toString());
        level.setData(ModAttachments.DISTORTION_ZONES, data);
        // 下发边界方块给施法者，客户端描边展示屏障范围
        ServerPlayer caster = level.getServer().getPlayerList().getPlayer(owner);
        if (caster != null) {
            PacketDistributor.sendToPlayer(caster,
                    new DistortionZoneSyncPacket(shellBlocks(min, max),
                            (int) Math.min(Short.MAX_VALUE, expiry - level.getGameTime())));
        }
    }

    /** 计算矩形区域外壳（表面）方块坐标，供客户端描边。 */
    private static List<BlockPos> shellBlocks(BlockPos min, BlockPos max) {
        List<BlockPos> out = new ArrayList<>();
        int x0 = min.getX(), x1 = max.getX();
        int y0 = min.getY(), y1 = max.getY();
        int z0 = min.getZ(), z1 = max.getZ();
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                out.add(new BlockPos(x, y, z0));
                if (z1 != z0) {
                    out.add(new BlockPos(x, y, z1));
                }
            }
            for (int z = z0 + 1; z < z1; z++) {
                out.add(new BlockPos(x0, y, z));
                if (x1 != x0) {
                    out.add(new BlockPos(x1, y, z));
                }
            }
        }
        return out;
    }

    /** 覆盖指定方块坐标的未过期扭曲区域（没有返回 {@code null}）。 */
    public static DistortionZoneData.Ring ringAt(ServerLevel level, BlockPos pos) {
        DistortionZoneData data = level.getData(ModAttachments.DISTORTION_ZONES);
        return data.covering(pos, level.getGameTime());
    }

    /** 削减扭曲区域耐久；返回削减后是否仍存活。 */
    public static boolean damageRing(ServerLevel level, BlockPos pos, int amount) {
        DistortionZoneData data = level.getData(ModAttachments.DISTORTION_ZONES);
        DistortionZoneData.Ring ring = data.covering(pos, level.getGameTime());
        if (ring == null) {
            return true;
        }
        boolean alive = ring.damage(amount);
        data.rings().removeIf(r -> r == ring);
        if (alive) {
            data.rings().add(ring.withHp(ring.hp() - amount));
        }
        level.setData(ModAttachments.DISTORTION_ZONES, data);
        return alive;
    }

    /**
     * 移除扭曲区域（门被再次打开 / 耐久耗尽）。
     *
     * @return 是否有区域被移除
     */
    public static boolean removeRingAt(ServerLevel level, BlockPos pos) {
        DistortionZoneData data = level.getData(ModAttachments.DISTORTION_ZONES);
        boolean removed = data.rings().removeIf(r -> r.covers(pos));
        if (removed) {
            level.setData(ModAttachments.DISTORTION_ZONES, data);
        }
        return removed;
    }

    /** 门方块是否已打开（用于「门被再次打开 → 屏障提前消失」）。 */
    public static boolean isDoorOpen(ServerLevel level, BlockPos doorPos) {
        return level.getBlockState(doorPos).getBlock() instanceof DoorBlock
                && level.getBlockState(doorPos).getValue(DoorBlock.OPEN);
    }

    /** 该坐标是否属于某个封闭屏障 / 隔绝房间的边界方块（供左键击破判定）。 */
    public static boolean isRingBoundary(ServerLevel level, BlockPos pos) {
        DistortionZoneData.Ring ring = ringAt(level, pos);
        if (ring == null) {
            return false;
        }
        BlockPos a = ring.minPos();
        BlockPos b = ring.maxPos();
        // 边界：任一轴坐标等于最小 / 最大，即位于表面
        return pos.getX() == a.getX() || pos.getX() == b.getX()
                || pos.getY() == a.getY() || pos.getY() == b.getY()
                || pos.getZ() == a.getZ() || pos.getZ() == b.getZ();
    }

    /** 判断某玩家是否可自由进入隔绝房间（施法者本人，或序列号更小 = 更高序列）。 */
    public static boolean canEnterIsolate(ServerLevel level, ServerPlayer visitor, BlockPos pos) {
        DistortionZoneData.Ring ring = ringAt(level, pos);
        if (ring == null || ring.type() != DistortionZoneData.TYPE_ISOLATE) {
            return true;
        }
        String ownerStr = ring.owner();
        if (ownerStr.equals(visitor.getUUID().toString())) {
            return true;
        }
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(UUID.fromString(ownerStr));
        if (owner == null) {
            return true; // 施法者不在线，区域失去约束力
        }
        int ownerSeq = owner.getData(ModAttachments.SEQUENCE_LEVEL);
        int visitorSeq = visitor.getData(ModAttachments.SEQUENCE_LEVEL);
        // 序列号更小 = 序列更高，可无视隔绝
        return visitorSeq > 0 && visitorSeq < ownerSeq;
    }

    /** 判断坐标是否位于隔绝房间内部（供进出判定）。 */
    public static boolean isInsideIsolate(ServerLevel level, BlockPos pos) {
        DistortionZoneData.Ring ring = ringAt(level, pos);
        return ring != null && ring.type() == DistortionZoneData.TYPE_ISOLATE;
    }

    /** 门方块的半高（上半部分 / 下半部分判定用）。 */
    public static boolean isUpperHalf(ServerLevel level, BlockPos doorPos) {
        return level.getBlockState(doorPos).getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER;
    }
}