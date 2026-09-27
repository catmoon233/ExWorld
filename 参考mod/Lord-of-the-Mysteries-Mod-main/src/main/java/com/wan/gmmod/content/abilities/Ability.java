package com.wan.gmmod.content.abilities;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * 序列能力基类。
 * <p>
 * 除唯一 {@link #id} 外，能力还携带技能栏所需的元数据：
 * <ul>
 *     <li>{@link #spiritualityCost}：触发消耗的灵性；</li>
 *     <li>{@link #cooldownTicks}：冷却时间（游戏刻）；</li>
 *     <li>{@link #active}：是否为主动能力（仅主动能力可放入技能栏并被快捷键触发）。</li>
 * </ul>
 * 图标纹理与显示名默认按 {@link #id} 推断，子类可覆盖。
 */
public abstract class Ability {
    protected final ResourceLocation id;
    /** 触发消耗的灵性值（被动能力通常为 0） */
    protected final int spiritualityCost;
    /** 冷却时间（游戏刻，20 tick = 1 秒） */
    protected final int cooldownTicks;
    /** 是否为主动能力：可配置进技能栏并由快捷键触发 */
    protected final boolean active;

    /** 兼容旧构造：被动能力，无消耗、无冷却。 */
    public Ability(ResourceLocation id) {
        this(id, 0, 0, false);
    }

    public Ability(ResourceLocation id, int spiritualityCost, int cooldownTicks, boolean active) {
        this.id = id;
        this.spiritualityCost = spiritualityCost;
        this.cooldownTicks = cooldownTicks;
        this.active = active;
    }

    public ResourceLocation getId() { return id; }

    public int getSpiritualityCost() { return spiritualityCost; }

    public int getCooldownTicks() { return cooldownTicks; }

    public boolean isActive() { return active; }

    /**
     * 能力图标纹理。默认约定：{@code <namespace>:textures/gui/skills/<path>.png}。
     * 玩家自行添加对应 PNG 即可，缺失时会渲染为「缺失纹理」占位而不崩溃。
     */
    public ResourceLocation getIconTexture() {
        return ResourceLocation.fromNamespaceAndPath(id.getNamespace(),
                "textures/gui/skills/" + id.getPath() + ".png");
    }

    /** 显示名翻译键，默认 {@code ability.<namespace>.<path>}。 */
    public String getNameKey() {
        return "ability." + id.getNamespace() + "." + id.getPath();
    }

    /** 说明翻译键，默认 {@code ability.<namespace>.<path>.desc}。 */
    public String getDescriptionKey() {
        return getNameKey() + ".desc";
    }

    /** 被动效果，在 PlayerTickEvent 中调用 */
    public void onPassiveTick(Player player) {}

    /** 主动能力触发（从网络包调用，运行在服务端） */
    public void onActivate(Player player) {}

    /** 当能力被移除或替换时调用 */
    public void onDeactivate(Player player) {}
}
