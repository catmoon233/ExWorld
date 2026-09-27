package com.wan.gmmod.client.gui;

import com.wan.gmmod.GuimiMod;
import com.wan.gmmod.client.HudClientState;
import com.wan.gmmod.client.quest.QuestClientState;
import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.common.capability.data.QuestData;
import com.wan.gmmod.content.quest.Task;
import com.wan.gmmod.content.quest.TaskObjective;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * 任务追踪 HUD：在屏幕右侧绘制被追踪任务的名称与目标进度（最多 3 个）。
 * <p>
 * 追踪列表存于 {@link QuestData#tracked}，由服务端同步；任务元数据来自
 * {@link QuestClientState}（登录时由 {@code QuestSyncPacket} 下发）。
 * 名称按任务类型着色，已完成目标绿色对勾。
 */
@EventBusSubscriber(modid = GuimiMod.MODID, value = Dist.CLIENT)
public class QuestTrackHudOverlay {

    @SubscribeEvent
    public static void registerLayer(RegisterGuiLayersEvent event) {
        // Quests are disabled.
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui || HudClientState.isHidden()) {
            return;
        }
        QuestData data = player.getData(ModAttachments.QUEST_DATA);
        if (data.tracked().isEmpty()) {
            return;
        }

        int screenW = mc.getWindow().getGuiScaledWidth();
        int x = screenW - 170;
        int y = 24;

        for (String taskId : data.tracked()) {
            Task task = QuestClientState.get(taskId);
            if (task == null) {
                continue;
            }
            // 背景
            graphics.fill(x - 4, y - 2, x + 166, y + 20 * task.objectives().size() + 14, 0x66000000);

            // 标题（类型色）
            Component title = Component.translatable(task.name());
            String clipped = mc.font.plainSubstrByWidth(title.getString(), 160);
            graphics.drawString(mc.font, clipped, x, y, task.type().getColor(), false);
            y += 10;

            // 目标进度
            for (int i = 0; i < task.objectives().size(); i++) {
                TaskObjective obj = task.objectives().get(i);
                int cur = data.progressOf(TaskObjective.progressKey(taskId, i));
                boolean done = cur >= obj.count();
                String line = objectiveLine(task, obj, cur);
                graphics.drawString(mc.font, line, x + 6, y, done ? 0x66FF66 : 0xFFFFFF, false);
                y += 10;
            }
            y += 6;
        }
    }

    private static String objectiveLine(Task task, TaskObjective obj, int cur) {
        switch (obj.type()) {
            case "kill":
            case "collect":
            case "craft":
            case "ability":
                return "\u2022 " + Component.translatable(
                        "gui.guimi_mod.quest.obj." + obj.type(), obj.target(), obj.count(), cur).getString();
            case "explore":
            case "promote":
                return "\u2022 " + Component.translatable(
                        "gui.guimi_mod.quest.obj." + obj.type(), obj.target()).getString();
            default:
                return "\u2022 " + obj.target() + " " + cur + "/" + obj.count();
        }
    }
}