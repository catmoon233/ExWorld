package com.wan.gmmod.content.disguise;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 可变形的人形怪物预设表（无面人 · 序列 6）。
 * <p>
 * 只有列于本表的怪物才被视为「人形」，可被击杀 / 观察解锁并用于变形；
 * 爬行者、史莱姆等非人形怪物不在表内，无法变形。判断标准即「是否在本预设表中」
 * （这些怪物的模型均基于 {@code HumanoidModel}）。
 * <p>
 * 每项携带一个显示缩放系数：由于变形不改变玩家 1.8 格碰撞箱，
 * 而部分怪物模型高矮不一（如末影人偏高、凋灵骷髅偏高），故按视觉高度轻微缩放适配。
 */
public final class HumanoidDisguises {
    private HumanoidDisguises() {}

    /**
     * 单条人形怪物外观定义。
     *
     * @param type            怪物实体类型
     * @param scale           渲染缩放系数（1.0 = 与玩家等高）
     * @param neutralToZombies 变形后是否让附近僵尸类保持中立
     * @param neutralToPiglins 变形后是否让附近猪灵类保持中立
     */
    public record Entry(EntityType<?> type, float scale, boolean neutralToZombies, boolean neutralToPiglins) {
        public ResourceLocation id() {
            return EntityType.getKey(type);
        }
    }

    /** 预设表：ID -> 定义，保持声明顺序（供图鉴按序展示）。 */
    private static final Map<ResourceLocation, Entry> ENTRIES = new LinkedHashMap<>();

    /** 初始赠送的基础外观（刚晋升序列 6 时给予）。 */
    public static final ResourceLocation[] INITIAL_GIFTS = {
            EntityType.getKey(EntityType.ZOMBIE),
            EntityType.getKey(EntityType.SKELETON)
    };

    static {
        // 僵尸家族（对僵尸中立）
        add(EntityType.ZOMBIE, 1.0F, true, false);
        add(EntityType.HUSK, 1.0F, true, false);
        add(EntityType.DROWNED, 1.0F, true, false);
        add(EntityType.ZOMBIE_VILLAGER, 1.0F, true, false);
        // 骷髅家族
        add(EntityType.SKELETON, 1.0F, false, false);
        add(EntityType.STRAY, 1.0F, false, false);
        add(EntityType.WITHER_SKELETON, 0.75F, false, false);
        // 灾厄家族
        add(EntityType.PILLAGER, 1.0F, false, false);
        add(EntityType.VINDICATOR, 1.0F, false, false);
        add(EntityType.EVOKER, 1.0F, false, false);
        add(EntityType.ILLUSIONER, 1.0F, false, false); // 灾厄村民变种
        add(EntityType.WITCH, 1.0F, false, false);
        // 猪灵家族（对猪灵中立）
        add(EntityType.PIGLIN, 1.0F, false, true);
        add(EntityType.ZOMBIFIED_PIGLIN, 1.0F, false, true);
        add(EntityType.PIGLIN_BRUTE, 1.0F, false, true);
        // 末影人（中立但人形）：偏高，缩放适配
        add(EntityType.ENDERMAN, 0.62F, false, false);
    }

    private static void add(EntityType<?> type, float scale, boolean neutralToZombies, boolean neutralToPiglins) {
        ENTRIES.put(EntityType.getKey(type), new Entry(type, scale, neutralToZombies, neutralToPiglins));
    }

    /** 全部预设（保持声明顺序）。 */
    public static Iterable<Entry> all() {
        return ENTRIES.values();
    }

    /** 按 ID 取定义，不存在返回 {@code null}。 */
    public static Entry get(ResourceLocation id) {
        return id == null ? null : ENTRIES.get(id);
    }

    /** 该实体类型是否为可变形的人形怪物。 */
    public static boolean isHumanoid(EntityType<?> type) {
        return type != null && ENTRIES.containsKey(EntityType.getKey(type));
    }

    /** 该 ID 是否为可变形的人形怪物。 */
    public static boolean isHumanoid(ResourceLocation id) {
        return id != null && ENTRIES.containsKey(id);
    }
}
