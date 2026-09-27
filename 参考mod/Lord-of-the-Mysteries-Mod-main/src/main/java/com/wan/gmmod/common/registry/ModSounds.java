package com.wan.gmmod.common.registry;

import com.wan.gmmod.GuimiMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * 模组音效注册表。
 * <p>
 * 目前仅注册「幻听」音效——当玩家灵性长期枯竭时，
 * 由 {@code GameEventSubscriber} 单独发送给该玩家本人（个人化幻听）。
 * <p>
 * 音频文件与 {@code sounds.json} 由用户自行提供：
 * 在 {@code assets/guimi_mod/sounds.json} 中定义键 {@code "hallucination"}，
 * 指向 {@code assets/guimi_mod/sounds/} 下的 .ogg 文件（可放多条实现随机变体）。
 */
public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, GuimiMod.MODID);

    /** 幻听音效 */
    public static final Supplier<SoundEvent> HALLUCINATION =
            SOUND_EVENTS.register("hallucination",
                    () -> SoundEvent.createVariableRangeEvent(GuimiMod.id("hallucination")));

    /** 美人鱼的歌声：美人鱼唱歌技能的环境音效（音频文件由用户自备） */
    public static final Supplier<SoundEvent> MERMAID_SONG =
            SOUND_EVENTS.register("mermaid_song",
                    () -> SoundEvent.createVariableRangeEvent(GuimiMod.id("mermaid_song")));

    /** 疯狂低语：失控状态下替换环境音效，单独发给失控玩家（音频文件由用户自备） */
    public static final Supplier<SoundEvent> MAD_WHISPER =
            SOUND_EVENTS.register("mad_whisper",
                    () -> SoundEvent.createVariableRangeEvent(GuimiMod.id("mad_whisper")));

    /** 丧钟开枪音效（音频文件由用户自备） */
    public static final Supplier<SoundEvent> GUN_FIRE =
            SOUND_EVENTS.register("gun_fire",
                    () -> SoundEvent.createVariableRangeEvent(GuimiMod.id("gun_fire")));

    /** 丧钟换弹音效（音频文件由用户自备） */
    public static final Supplier<SoundEvent> GUN_RELOAD =
            SOUND_EVENTS.register("gun_reload",
                    () -> SoundEvent.createVariableRangeEvent(GuimiMod.id("gun_reload")));

    /** 丧钟空枪击锤音效（音频文件由用户自备） */
    public static final Supplier<SoundEvent> GUN_EMPTY =
            SOUND_EVENTS.register("gun_empty",
                    () -> SoundEvent.createVariableRangeEvent(GuimiMod.id("gun_empty")));

    /** 狼人攻击咆哮音效（音频文件由用户提供，已转 ogg 置于 sounds/） */
    public static final Supplier<SoundEvent> WOLFMAN_ATTACK =
            SOUND_EVENTS.register("wolfman_attack",
                    () -> SoundEvent.createVariableRangeEvent(GuimiMod.id("wolfman_attack")));

    public static void register(IEventBus bus) {
        SOUND_EVENTS.register(bus);
    }
}
