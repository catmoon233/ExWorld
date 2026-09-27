package com.wan.gmmod.client;

import com.wan.gmmod.common.network.packet.DistortionCastPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 扭曲相关客户端状态管理器（仅客户端）。
 * <p>
 * 三部分职责：
 * <ul>
 *   <li><b>扭曲模式</b>：收到 {@code DistortionModeSyncPacket} 后通过
 *   {@link #setModeActive(boolean)} 切换 UI 激活态，并维护本地倒计时
 *   （与模式窗口 15 秒等长）自动关闭；</li>
 *   <li><b>扭曲选型</b>：扭曲模式下用 1~6 键选择当前扭曲类型，左键确认目标 /
 *   右键拖拽区域，组装 {@code DistortionCastPacket} 发往服务端；</li>
 *   <li><b>区域描边</b>：收到 {@code DistortionZoneSyncPacket} 时通过
 *   {@link #addZoneOutlines(java.util.Collection, int)} 登记需要描边的扭曲区域边界方块，
 *   每客户端 tick 调用 {@link #tick()} 递减计时。</li>
 * </ul>
 * 渲染层通过 {@link #positions()} 获取当前需要描边的方块（淡紫色）。
 */
public final class DistortionClientState {
    /** 扭曲模式窗口时长（tick，与服务端一致） */
    private static final int MODE_DURATION_TICKS = 15 * 20;
    /** 实体目标拾取距离（米） */
    private static final double ENTITY_PICK_RANGE = 32.0;

    /** 当前是否处于扭曲模式（本地 UI 态） */
    private static volatile boolean modeActive;
    /** 模式剩余 tick 数（本地倒计时） */
    private static int modeTicksLeft;

    /** 当前选中的扭曲类型（{@link DistortionCastPacket} T_* 常量） */
    private static int selectedType = DistortionCastPacket.T_MOVE_INVERT;

    /** 右键拖拽：是否正在拖拽区域 */
    private static boolean dragging;
    /** 拖拽起点方块（右键按下瞬间的准星方块） */
    private static BlockPos dragStart;

    /** 边界方块坐标 -> 剩余持续 tick */
    private static final Map<BlockPos, Integer> OUTLINES = new ConcurrentHashMap<>();

    private DistortionClientState() {
    }

    // =====================================================================
    // 扭曲模式
    // =====================================================================

    /** 是否处于扭曲模式（UI / 输入拦截判定用）。 */
    public static boolean isModeActive() {
        return modeActive;
    }

    /** 进入 / 退出扭曲模式：同步并启动本地倒计时，重置选型与拖拽。 */
    public static void setModeActive(boolean active) {
        modeActive = active;
        modeTicksLeft = active ? MODE_DURATION_TICKS : 0;
        if (!active) {
            dragging = false;
            dragStart = null;
        }
    }

    /** 模式剩余 tick 数（供 HUD 显示剩余秒数）。 */
    public static int remainingTicks() {
        return Math.max(0, modeTicksLeft);
    }

    // =====================================================================
    // 扭曲选型与施放交互
    // =====================================================================

    /** 当前选中的扭曲类型。 */
    public static int getSelectedType() {
        return selectedType;
    }

    /** 数字键 1~6 切换扭曲类型（0~5）。 */
    public static void setSelectedType(int index) {
        selectedType = Math.max(0, Math.min(5, index));
    }

    /** 左键确认：按当前类型拾取目标并发送施放包。返回是否真正施放。 */
    public static boolean fireAttack() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null || !modeActive) {
            return false;
        }
        if (selectedType == DistortionCastPacket.T_DEFLECT) {
            // 弹射物偏转：无需目标，直接施放
            PacketDistributor.sendToServer(
                    new DistortionCastPacket(DistortionCastPacket.T_DEFLECT, -1, null, null, null));
            return true;
        }
        if (selectedType == DistortionCastPacket.T_SEAL_DOOR) {
            // 封闭屏障：拾取门方块
            BlockPos door = doorAtCursor(mc);
            if (door == null) {
                return false;
            }
            PacketDistributor.sendToServer(
                    new DistortionCastPacket(DistortionCastPacket.T_SEAL_DOOR, -1, door, null, null));
            return true;
        }
        // 其余三种均为实体目标：移动反向 / 攻击转移 / 占卜劫持
        int entityId = entityAtCursor(mc);
        if (entityId <= 0) {
            return false;
        }
        PacketDistributor.sendToServer(
                new DistortionCastPacket(selectedType, entityId, null, null, null));
        return true;
    }

    /** 右键按下：开始记录拖拽起点（区域选区）。 */
    public static void startDrag() {
        Minecraft mc = Minecraft.getInstance();
        BlockPos cursor = cursorBlock(mc);
        if (cursor != null) {
            dragging = true;
            dragStart = cursor;
        }
    }

    /** 右键松开：若拖拽出区域则发送隔绝房间包。 */
    public static void endDrag() {
        if (!dragging) {
            return;
        }
        dragging = false;
        BlockPos start = dragStart;
        dragStart = null;
        Minecraft mc = Minecraft.getInstance();
        BlockPos end = cursorBlock(mc);
        if (start == null || end == null) {
            return;
        }
        if (start.equals(end)) {
            return;
        }
        BlockPos min = new BlockPos(
                Math.min(start.getX(), end.getX()),
                Math.min(start.getY(), end.getY()),
                Math.min(start.getZ(), end.getZ()));
        BlockPos max = new BlockPos(
                Math.max(start.getX(), end.getX()),
                Math.max(start.getY(), end.getY()),
                Math.max(start.getZ(), end.getZ()));
        PacketDistributor.sendToServer(
                new DistortionCastPacket(DistortionCastPacket.T_ISOLATE, -1, null, min, max));
    }

    /** 拖拽期间每 tick 更新当前拖拽终点（供 HUD 预览区域）。 */
    public static BlockPos dragCurrent() {
        if (!dragging) {
            return null;
        }
        return cursorBlock(Minecraft.getInstance());
    }

    /** 准星指向的门方块坐标，没有指向门返回 {@code null}。 */
    private static BlockPos doorAtCursor(Minecraft mc) {
        BlockPos pos = cursorBlock(mc);
        if (pos == null) {
            return null;
        }
        BlockState state = mc.level.getBlockState(pos);
        return state.getBlock() instanceof DoorBlock ? pos : null;
    }

    /** 准星指向的实体目标 id，没有返回 -1。 */
    private static int entityAtCursor(Minecraft mc) {
        net.minecraft.client.player.LocalPlayer player = mc.player;
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = start.add(look.scale(ENTITY_PICK_RANGE));
        net.minecraft.world.phys.AABB box =
                player.getBoundingBox().expandTowards(look.scale(ENTITY_PICK_RANGE)).inflate(1.0);
        net.minecraft.world.phys.EntityHitResult hit =
                net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                        player, start, end, box,
                        e -> e != player && e instanceof net.minecraft.world.entity.LivingEntity
                                && e.isPickable(),
                        ENTITY_PICK_RANGE * ENTITY_PICK_RANGE);
        return hit == null ? -1 : hit.getEntity().getId();
    }

    /** 准星所在方块坐标（包含空气），无方块返回 {@code null}。 */
    private static BlockPos cursorBlock(Minecraft mc) {
        if (mc.level == null || mc.hitResult == null) {
            return null;
        }
        return mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult bhr
                ? bhr.getBlockPos() : null;
    }

    // =====================================================================
    // 区域描边
    // =====================================================================

    /**
     * 登记一批扭曲区域边界方块。
     *
     * @param positions 边界方块坐标集合
     * @param ticks     持续 tick 数
     */
    public static void addZoneOutlines(java.util.Collection<BlockPos> positions, int ticks) {
        for (BlockPos pos : positions) {
            OUTLINES.put(pos.immutable(), ticks);
        }
    }

    /**
     * 当前需要描边的扭曲边界方块集合（渲染层遍历使用）。
     */
    public static java.util.Set<BlockPos> positions() {
        return OUTLINES.keySet();
    }

    /**
     * 每客户端 tick 调用一次：递减模式倒计时与描边计时，到期后关闭 / 移除。
     */
    public static void tick() {
        if (modeActive) {
            modeTicksLeft--;
            if (modeTicksLeft <= 0) {
                modeActive = false;
                modeTicksLeft = 0;
                dragging = false;
                dragStart = null;
            }
        }
        Iterator<Map.Entry<BlockPos, Integer>> it = OUTLINES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, Integer> entry = it.next();
            int remaining = entry.getValue() - 1;
            if (remaining <= 0) {
                it.remove();
            } else {
                entry.setValue(remaining);
            }
        }
    }
}