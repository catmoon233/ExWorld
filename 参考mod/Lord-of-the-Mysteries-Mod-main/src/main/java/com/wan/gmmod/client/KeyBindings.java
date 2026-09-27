package com.wan.gmmod.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.wan.gmmod.GuimiMod;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = GuimiMod.MODID, value = Dist.CLIENT)
public class KeyBindings {
    /** 技能槽触发键数量：与每页槽位数一致，键 1~5 映射到当前技能页的 5 个槽位 */
    public static final int SKILL_SLOT_COUNT = SkillPageClientState.SLOTS_PER_PAGE;

    // V 键：切换灵视（提供夜视 + 看见灵体）
    public static final KeyMapping SPIRIT_VISION_KEY = new KeyMapping(
            "key.guimi_mod.spirit_vision",         // 翻译键
            KeyConflictContext.IN_GAME,            // 只在游戏中生效
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,                      // V 键
            "key.categories.guimi_mod"             // 分类
    );

    // K 键：打开 / 关闭技能配置界面
    public static final KeyMapping OPEN_SKILL_CONFIG = new KeyMapping(
            "key.guimi_mod.skill_config",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            "key.categories.guimi_mod"
    );

    // X 键：灵性之墙（需要手持仪式匕首）
    public static final KeyMapping SPIRIT_WALL_KEY = new KeyMapping(
            "key.guimi_mod.spirit_wall",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_X,
            "key.categories.guimi_mod"
    );

    // P 键：冥想
    public static final KeyMapping MEDITATION_KEY = new KeyMapping(
            "key.guimi_mod.meditation",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_P,
            "key.categories.guimi_mod"
    );

    // 变形选择界面（无面人 · 序列 6）：默认不绑定，由玩家在控制设置中自行分配
    public static final KeyMapping OPEN_DISGUISE_KEY = new KeyMapping(
            "key.guimi_mod.disguise",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            "key.categories.guimi_mod"
    );

    // H 键：隐藏 / 显示模组 HUD
    public static final KeyMapping TOGGLE_HUD_KEY = new KeyMapping(
            "key.guimi_mod.toggle_hud",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            "key.categories.guimi_mod"
    );

    // 纸牌发射模式切换（精准单点 / 散射）：默认不绑定，由玩家在控制设置中自行分配
    public static final KeyMapping CARD_MODE_KEY = new KeyMapping(
            "key.guimi_mod.card_mode",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            "key.categories.guimi_mod"
    );

    // J 键：打开任务书
    public static final KeyMapping OPEN_QUEST_JOURNAL = new KeyMapping(
            "key.guimi_mod.quest_journal",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            "key.categories.guimi_mod"
    );

    // 5 个技能槽触发键（映射到当前技能页），默认不绑定（GLFW_KEY_UNKNOWN），由玩家在控制设置中自行分配
    public static final KeyMapping[] SKILL_SLOTS = new KeyMapping[SKILL_SLOT_COUNT];

    static {
        for (int i = 0; i < SKILL_SLOT_COUNT; i++) {
            SKILL_SLOTS[i] = new KeyMapping(
                    "key.guimi_mod.skill_" + (i + 1),
                    KeyConflictContext.IN_GAME,
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_UNKNOWN,
                    "key.categories.guimi_mod"
            );
        }
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(SPIRIT_VISION_KEY);
        event.register(SPIRIT_WALL_KEY);
        event.register(MEDITATION_KEY);
        event.register(OPEN_DISGUISE_KEY);
        event.register(TOGGLE_HUD_KEY);
        event.register(CARD_MODE_KEY);
        event.register(OPEN_QUEST_JOURNAL);
        for (KeyMapping slot : SKILL_SLOTS) {
            event.register(slot);
        }
    }
}
