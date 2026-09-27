package com.wan.gmmod.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 蛛丝蚕茧的客户端状态：由 {@link com.wan.gmmod.common.network.packet.CocoonSyncPacket} 驱动。
 * <p>
 * 记录哪些实体正处于蚕茧包裹中及其剩余时长，供世界渲染层绘制半透明外壳 / 地面蛛丝圆阵，
 * 供第一人称滤色层对本地玩家叠加蛛丝视觉。火焰破茧时以烧毁状态短暂残留，供红色滤网渐隐。
 */
public final class CocoonClientState {

    /** 单实体的蚕茧状态：剩余时长（刻）+ 是否正被火烧破。 */
    public record Entry(int remainingTicks, boolean burning) {}

    private static final Map<Integer, Entry> ACTIVE = new HashMap<>();

    private CocoonClientState() {}

    /** 蚕茧覆盖：以指定剩余时长登记该实体（未被火烧破）。 */
    public static void enclose(int entityId, int ticks) {
        ACTIVE.put(entityId, new Entry(Math.max(1, ticks), false));
    }

    /** 火破蚕茧：以短暂时长进入烧毁状态，外壳与滤网转为红色渐隐。 */
    public static void burst(int entityId, int ticks) {
        ACTIVE.put(entityId, new Entry(Math.max(1, ticks), true));
    }

    /** 每客户端 tick 调用：递减所有进行中的蚕豆剩余时长，归零后移除。 */
    public static void tick() {
        if (ACTIVE.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<Integer, Entry>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Entry> e = it.next();
            Entry entry = e.getValue();
            int remaining = entry.remainingTicks() - 1;
            if (remaining <= 0) {
                it.remove();
            } else {
                e.setValue(new Entry(remaining, entry.burning()));
            }
        }
    }

    public static Entry get(int id) {
        return ACTIVE.get(id);
    }

    public static boolean isActive(int id) {
        return ACTIVE.containsKey(id);
    }

    public static Map<Integer, Entry> snapshot() {
        return new HashMap<>(ACTIVE);
    }
}