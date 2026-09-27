package com.wan.gmmod.common.registry;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.content.effects.AntiDivinationEffect;
import com.wan.gmmod.content.effects.BatFormEffect;
import com.wan.gmmod.content.effects.BlackFlameEffect;
import com.wan.gmmod.content.effects.BleedingEffect;
import com.wan.gmmod.content.effects.DawnMarkEffect;
import com.wan.gmmod.content.effects.DemonFormEffect;
import com.wan.gmmod.content.effects.EnragedEffect;
import com.wan.gmmod.content.effects.FallCorruptionEffect;
import com.wan.gmmod.content.effects.WerewolfFormEffect;
import com.wan.gmmod.content.effects.LosingControlEffect;
import com.wan.gmmod.content.effects.MermaidSongEffect;
import com.wan.gmmod.content.effects.SealedCorruptionEffect;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 模组状态效果注册表。
 * <ul>
 *     <li>{@link #MERMAID_SONG}：「美人鱼的歌声」隐藏 Buff（无图标），作为晋升仪式的环境标记。
 *     Buff 真伪由服务端附件 {@code MERMAID_SONG_GENUINE} 悄悄判定，不告知玩家。</li>
 *     <li>{@link #LOSING_CONTROL}：失控效果——仪式条件不满足时服用高序列魔药的惩罚，
 *     期间无法使用序列能力、随机瞬移并攻击附近生物、耳边响起疯狂低语。</li>
 * </ul>
 */
public class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(BuiltInRegistries.MOB_EFFECT, GuimiMod.MODID);

    /** 美人鱼的歌声（隐藏 Buff，仪式检测标记） */
    public static final DeferredHolder<MobEffect, MobEffect> MERMAID_SONG =
            EFFECTS.register("mermaid_song", MermaidSongEffect::new);

    /** 失控（高序列晋升失败的惩罚） */
    public static final DeferredHolder<MobEffect, MobEffect> LOSING_CONTROL =
            EFFECTS.register("losing_control", LosingControlEffect::new);

    /** 切割流血（散射纸牌命中附加，每秒 0.5 伤害） */
    public static final DeferredHolder<MobEffect, MobEffect> BLEEDING =
            EFFECTS.register("bleeding", BleedingEffect::new);

    /** 激怒（挑衅者「挑衅」：锁定挑衅者 + 移速 +20% + 命中率 -30%） */
    public static final DeferredHolder<MobEffect, MobEffect> ENRAGED =
            EFFECTS.register("enraged", EnragedEffect::new);

    /** 反占卜（隐藏 Buff，自我反占卜标记，占卜逻辑据此干扰结果） */
    public static final DeferredHolder<MobEffect, MobEffect> ANTI_DIVINATION =
            EFFECTS.register("anti_divination", AntiDivinationEffect::new);

    /** 黑焰灼烧：被黑焰命中后的持续燃烧伤害（黑色火焰粒子吸附在身上） */
    public static final DeferredHolder<MobEffect, MobEffect> BLACK_FLAME_BURN =
            EFFECTS.register("black_flame_burn", BlackFlameEffect::new);

    /** 黎明命甲生效标记：驱动客户端渲染「黎明命甲」铠甲模型 */
    public static final DeferredHolder<MobEffect, MobEffect> DAWN_ARMOR_ACTIVE =
            EFFECTS.register("dawn_armor_active", DawnMarkEffect::new);

    /** 晨曦之剑生效标记：驱动客户端渲染「晨曦之剑」光剑模型 */
    public static final DeferredHolder<MobEffect, MobEffect> DAWN_SWORD_ACTIVE =
            EFFECTS.register("dawn_sword_active", DawnMarkEffect::new);

    /** 封印侵蚀：持有 / 穿戴封印物时承受的持续伤害代价 */
    public static final DeferredHolder<MobEffect, MobEffect> SEALED_CORRUPTION =
            EFFECTS.register("sealed_corruption", SealedCorruptionEffect::new);

    /** 深渊化：「堕落之物」范围攻击赋予的堕落状态，期间替身类能力失效 */
    public static final DeferredHolder<MobEffect, MobEffect> FALL_CORRUPTION =
            EFFECTS.register("fall_corruption", FallCorruptionEffect::new);

    /** 狼人化生效标记：驱动客户端渲染「狼人化」叠加模型（被缚者 · 序列7 狼人） */
    public static final DeferredHolder<MobEffect, MobEffect> WEREWOLF_FORM =
            EFFECTS.register("werewolf_form", WerewolfFormEffect::new);

    /** 恶魔化生效标记：驱动客户端渲染「恶魔化」全身模型（深渊 · 序列6 恶魔） */
    public static final DeferredHolder<MobEffect, MobEffect> DEMON_FORM =
            EFFECTS.register("demon_form", DemonFormEffect::new);

    /** 蝙蝠化形生效标记：驱动客户端隐藏玩家本体模型（化身蝙蝠群，月亮 · 序列7 吸血鬼） */
    public static final DeferredHolder<MobEffect, MobEffect> BAT_FORM =
            EFFECTS.register("bat_form", BatFormEffect::new);

    public static void register(IEventBus bus) {
        EFFECTS.register(bus);
    }
}
