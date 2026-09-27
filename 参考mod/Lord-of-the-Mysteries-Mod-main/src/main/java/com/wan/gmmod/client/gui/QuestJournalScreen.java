package com.wan.gmmod.client.gui;

import com.wan.gmmod.client.quest.QuestClientState;
import com.wan.gmmod.common.capability.ModAttachments;
import com.wan.gmmod.common.capability.data.QuestData;
import com.wan.gmmod.common.network.packet.QuestActionPacket;
import com.wan.gmmod.content.quest.Task;
import com.wan.gmmod.content.quest.TaskConditions;
import com.wan.gmmod.content.quest.TaskObjective;
import com.wan.gmmod.content.quest.TaskType;
import com.wan.gmmod.content.sequences.Sequences;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * 任务书界面（J 键打开）。
 * <p>
 * 布局：
 * <ul>
 *   <li><b>背景</b>：128×96 背景（quest_background.png）拉伸铺满面板；</li>
 *   <li><b>左侧列表</b>：主线任务（story=true），按玩家当前途径过滤，可滚动选择；</li>
 *   <li><b>右侧列表</b>：支线任务（story=false），同样按当前途径过滤；</li>
 *   <li><b>底部详情</b>：选中任务的名称、描述、目标进度、奖励、解锁条件；</li>
 *   <li><b>最底按钮</b>：按任务状态显示「接取 / 放弃 / 追踪（取消追踪）」。</li>
 * </ul>
 * 操作均发送 {@link QuestActionPacket} 到服务端执行，进度由附件同步刷新。
 */
public class QuestJournalScreen extends Screen {
    private static final int TEX_W = 1280;
    private static final int TEX_H = 704;
    private static final int PANEL_W = 640;
    private static final int PANEL_H = 320;

    private static final int COL_X1 = 16;
    private static final int COL_X2 = 334;
    private static final int COL_W = 292;
    private static final int LIST_HEADER_Y = 30;
    private static final int LIST_TOP = 46;
    private static final int ROW_H = 18;
    private static final int LIST_VISIBLE = 6;
    private static final int DETAIL_Y = 158;
    private static final int DETAIL_BOTTOM = 288;
    private static final int BTN_Y = 294;

    private static final ResourceLocation BACKGROUND =
            ResourceLocation.fromNamespaceAndPath("guimi_mod", "textures/gui/quest_background.png");

    private int scrollMain;
    private int scrollSide;
    private int scrollDetail;
    private String selected;

    // 响应式缩放：让整个面板随窗口大小缩放，小窗口完整显示、大窗口占满
    private float uiScale;
    private int px, py, pw, ph;

    /** 依据当前窗口尺寸计算缩放与居中坐标。 */
    private void computeLayout() {
        this.uiScale = Math.max(0.25F, Math.min(1.75F,
                Math.min(this.width / (float) PANEL_W, this.height / (float) PANEL_H)));
        this.pw = Math.max(1, Math.round(PANEL_W * this.uiScale));
        this.ph = Math.max(1, Math.round(PANEL_H * this.uiScale));
        this.px = (this.width - this.pw) / 2;
        this.py = (this.height - this.ph) / 2;
    }

    /** 把屏幕坐标换算成面板本地坐标。 */
    private int toLocalX(double sx) {
        return (int) ((sx - this.px) / this.uiScale);
    }

    private int toLocalY(double sy) {
        return (int) ((sy - this.py) / this.uiScale);
    }

    public QuestJournalScreen() {
        super(Component.translatable("gui.guimi_mod.quest_journal.title"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** 按玩家当前途径过滤的任务（途径为空即不限制；主线/支线由 story 区分）。 */
    private static List<Task> tasksFor(Player player) {
        String pathway = player.getData(ModAttachments.PATHWAY);
        return QuestClientState.all().stream()
                .filter(t -> t.conditions().pathway().isEmpty()
                        || t.conditions().pathway().equals(pathway))
                .toList();
    }

    private static List<Task> mainTasks(Player player) {
        return tasksFor(player).stream()
                .filter(t -> t.story() || t.type() == TaskType.MAIN)
                .toList();
    }

    private static List<Task> sideTasks(Player player) {
        return tasksFor(player).stream()
                .filter(t -> !t.story() && t.type() != TaskType.MAIN)
                .toList();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        Player player = this.minecraft != null ? this.minecraft.player : null;
        if (player == null) {
            return;
        }
        computeLayout();
        int lmx = toLocalX(mouseX);
        int lmy = toLocalY(mouseY);

        // 当前任务背景图铺满整个界面（全屏拉伸），而非仅限面板
        graphics.blit(BACKGROUND, 0, 0, this.width, this.height,
                0, 0, TEX_W, TEX_H, TEX_W, TEX_H);
        // 全屏半透明蒙层，保证文字在任何背景下都清晰可读
        graphics.fill(0, 0, this.width, this.height, 0x40000000);

        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(this.px, this.py, 0F);
        pose.scale(this.uiScale, this.uiScale, 1F);

        graphics.drawCenteredString(this.font, this.title, PANEL_W / 2, 10, 0xFFD700);

        if (!QuestClientState.loaded()) {
            graphics.drawCenteredString(this.font,
                    Component.translatable("gui.guimi_mod.quest_journal.empty"),
                    PANEL_W / 2, PANEL_H / 2, 0xAAAAAA);
            pose.popPose();
            return;
        }

        QuestData data = player.getData(ModAttachments.QUEST_DATA);
        List<Task> main = mainTasks(player);
        List<Task> side = sideTasks(player);

        graphics.drawCenteredString(this.font,
                Component.translatable("gui.guimi_mod.quest.main"),
                COL_X1 + COL_W / 2, LIST_HEADER_Y, 0xFFD700);
        graphics.drawCenteredString(this.font,
                Component.translatable("gui.guimi_mod.quest.side"),
                COL_X2 + COL_W / 2, LIST_HEADER_Y, 0x55CCFF);

        drawColumn(graphics, player, data, main, true, lmx, lmy);
        drawColumn(graphics, player, data, side, false, lmx, lmy);

        Task selTask = this.selected == null ? null : QuestClientState.get(this.selected);
        if (selTask != null) {
            renderDetail(graphics, player, data, selTask);
        }
        pose.popPose();
    }

    /** 绘制一列任务列表（主/支线共用）。坐标已换算为面板本地坐标。 */
    private void drawColumn(GuiGraphics graphics, Player player, QuestData data,
                            List<Task> list, boolean main, int mouseX, int mouseY) {
        int scroll = main ? this.scrollMain : this.scrollSide;
        int maxScroll = Math.max(0, list.size() - LIST_VISIBLE);
        if (scroll > maxScroll) {
            scroll = maxScroll;
        }
        if (main) {
            this.scrollMain = scroll;
        } else {
            this.scrollSide = scroll;
        }
        int x = main ? COL_X1 : COL_X2;
        for (int i = 0; i < LIST_VISIBLE; i++) {
            int idx = i + scroll;
            if (idx >= list.size()) {
                break;
            }
            Task task = list.get(idx);
            int y = LIST_TOP + i * ROW_H;
            boolean hover = isInBox(mouseX, mouseY, x, y, COL_W, ROW_H);
            boolean done = data.hasCompleted(task.id().toString());
            boolean active = data.hasActive(task.id().toString());
            boolean isSel = task.id().toString().equals(this.selected);

            int bg;
            if (isSel) {
                bg = 0x88FFFFFF;
            } else if (hover) {
                bg = 0x55FFFFFF;
            } else if (active) {
                bg = 0x4400AA00;
            } else {
                bg = 0x44000000;
            }
            graphics.fill(x, y, x + COL_W, y + ROW_H, bg);

            int accent = main ? 0xFFD700 : 0x55CCFF;
            graphics.fill(x, y, x + 3, y + ROW_H, accent);

            String name = Component.translatable(task.name()).getString();
            if (done) {
                name = "\u2714 " + name;
            }
            String clipped = this.font.plainSubstrByWidth(name, COL_W - 14);
            graphics.drawString(this.font, clipped, x + 8, y + 5,
                    done ? 0x808080 : (active ? 0x66FF66 : 0xFFFFFF), false);
        }
    }

    /** 底部详情面板：名称、描述、目标、奖励、解锁条件（自动换行 + 可滚动）。 */
    private void renderDetail(GuiGraphics graphics, Player player, QuestData data,
                              Task task) {
        int x = 14;
        int x1 = PANEL_W - 14;
        graphics.fill(8, DETAIL_Y - 3, PANEL_W - 8, DETAIL_BOTTOM, 0x66000000);

        int lineY0 = DETAIL_Y;
        int bottom = DETAIL_BOTTOM - 2;
        int lineH = 9;
        int visible = Math.max(1, (bottom - lineY0) / lineH);
        int wrapW = x1 - x - 4;

        // 先把所有行收集起来，再按滚动偏移绘制可见部分，避免长文本被截断
        List<FormattedCharSequence> rows = new java.util.ArrayList<>();
        List<Integer> colors = new java.util.ArrayList<>();

        rows.add(Component.translatable(task.name()).getVisualOrderText());
        colors.add(task.type().getColor());
        String typeName = Component.translatable("gui.guimi_mod.quest.type." + task.type().getKey()).getString();
        rows.add(Component.literal("[" + typeName + "]").getVisualOrderText());
        colors.add(0xAAAAAA);

        for (FormattedCharSequence line : this.font.split(Component.translatable(task.description()), wrapW)) {
            rows.add(line);
            colors.add(0xCCCCCC);
        }

        rows.add(Component.translatable("gui.guimi_mod.quest.objectives").getVisualOrderText());
        colors.add(0xFFD700);
        for (int i = 0; i < task.objectives().size(); i++) {
            TaskObjective obj = task.objectives().get(i);
            int cur = data.progressOf(TaskObjective.progressKey(task.id().toString(), i));
            boolean done = cur >= obj.count();
            String line = objectiveText(task, obj, cur);
            for (FormattedCharSequence seq : this.font.split(Component.literal(line), wrapW)) {
                rows.add(seq);
                colors.add(done ? 0x66FF66 : 0xFFFFFF);
            }
        }

        if (!task.rewards().isEmpty()) {
            rows.add(Component.translatable("gui.guimi_mod.quest.rewards").getVisualOrderText());
            colors.add(0xFFD700);
            for (var reward : task.rewards()) {
                for (FormattedCharSequence seq : this.font.split(Component.literal(rewardText(reward)), wrapW)) {
                    rows.add(seq);
                    colors.add(0xDDDDDD);
                }
            }
        }

        TaskConditions c = task.conditions();
        if (hasCondition(c)) {
            rows.add(Component.translatable("gui.guimi_mod.quest.conditions").getVisualOrderText());
            colors.add(0xFFA500);
            for (String cond : conditionLines(player, data, c)) {
                for (FormattedCharSequence seq : this.font.split(Component.literal(cond), wrapW)) {
                    rows.add(seq);
                    colors.add(0xBBBBBB);
                }
            }
        }

        int maxScroll = Math.max(0, rows.size() - visible);
        if (this.scrollDetail > maxScroll) {
            this.scrollDetail = maxScroll;
        }
        int y = lineY0;
        for (int i = this.scrollDetail; i < rows.size() && y <= bottom; i++) {
            graphics.drawString(this.font, rows.get(i), x, y, colors.get(i), false);
            y += lineH;
        }
    }

    /** 任务是否有可展示的解锁条件。 */
    private static boolean hasCondition(TaskConditions c) {
        return !c.requiredQuest().isEmpty() || !c.pathway().isEmpty()
                || c.minSequence() > 0 || c.maxSequence() < 9 || !c.holdItem().isEmpty();
    }

    /** 客户端本地判断玩家是否能接取任务（与服务端 QuestManager 一致）。 */
    private static boolean canAccept(Player player, QuestData data, Task task) {
        TaskConditions c = task.conditions();
        if (!c.requiredQuest().isEmpty() && !data.hasCompleted(c.requiredQuest())) {
            return false;
        }
        if (!c.pathway().isEmpty() && !c.pathway().equals(player.getData(ModAttachments.PATHWAY))) {
            return false;
        }
        int lvl = player.getData(ModAttachments.SEQUENCE_LEVEL);
        if (lvl < c.minSequence() || lvl > c.maxSequence()) {
            return false;
        }
        if (!c.holdItem().isEmpty()) {
            boolean has = player.getInventory().items.stream()
                    .filter(s -> !s.isEmpty())
                    .anyMatch(s -> net.minecraft.core.registries.BuiltInRegistries.ITEM
                            .getKey(s.getItem()).toString().equals(c.holdItem()));
            if (!has) {
                return false;
            }
        }
        return true;
    }

    /** 生成条件展示行（满足打勾，未满足打叉）。 */
    private java.util.List<String> conditionLines(Player player, QuestData data, TaskConditions c) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        if (!c.requiredQuest().isEmpty()) {
            boolean ok = data.hasCompleted(c.requiredQuest());
            String label = taskName(c.requiredQuest());
            lines.add((ok ? "\u2714 " : "\u2717 ") + "前置: " + label);
        }
        if (!c.pathway().isEmpty()) {
            String cur = player.getData(ModAttachments.PATHWAY);
            boolean ok = c.pathway().equals(cur);
            Sequences.Pathway p = Sequences.fromKey(c.pathway());
            String name = p != null ? p.getDisplayName() : c.pathway();
            lines.add((ok ? "\u2714 " : "\u2717 ") + "途径: " + name);
        }
        if (c.minSequence() > 0 || c.maxSequence() < 9) {
            int lvl = player.getData(ModAttachments.SEQUENCE_LEVEL);
            boolean ok = lvl >= c.minSequence() && lvl <= c.maxSequence();
            lines.add((ok ? "\u2714 " : "\u2717 ") + "序列: " + c.minSequence() + " ~ " + c.maxSequence());
        }
        if (!c.holdItem().isEmpty()) {
            boolean ok = player.getInventory().items.stream()
                    .filter(s -> !s.isEmpty())
                    .anyMatch(s -> net.minecraft.core.registries.BuiltInRegistries.ITEM
                            .getKey(s.getItem()).toString().equals(c.holdItem()));
            lines.add((ok ? "\u2714 " : "\u2717 ") + "持有: " + c.holdItem());
        }
        return lines;
    }

    /** 取任务的已翻译名称（用于条件里的前置展示）。 */
    private String taskName(String id) {
        Task t = QuestClientState.get(id);
        if (t == null) {
            return id;
        }
        return Component.translatable(t.name()).getString();
    }

    private String objectiveText(Task task, TaskObjective obj, int cur) {
        String target = obj.target();
        switch (obj.type()) {
            case "kill":
                return "\u2022 " + Component.translatable("gui.guimi_mod.quest.obj.kill",
                        target, obj.count(), cur).getString();
            case "collect":
                return "\u2022 " + Component.translatable("gui.guimi_mod.quest.obj.collect",
                        target, obj.count(), cur).getString();
            case "craft":
                return "\u2022 " + Component.translatable("gui.guimi_mod.quest.obj.craft",
                        target, obj.count(), cur).getString();
            case "explore":
                return "\u2022 " + Component.translatable("gui.guimi_mod.quest.obj.explore",
                        target).getString();
            case "promote":
                return "\u2022 " + Component.translatable("gui.guimi_mod.quest.obj.promote",
                        target).getString();
            case "ability":
                return "\u2022 " + Component.translatable("gui.guimi_mod.quest.obj.ability",
                        target, obj.count(), cur).getString();
            default:
                return "\u2022 " + target + " " + cur + "/" + obj.count();
        }
    }

    private String rewardText(com.wan.gmmod.content.quest.TaskReward reward) {
        switch (reward.type()) {
            case "item":
                return "\u2022 " + Component.translatable("gui.guimi_mod.quest.reward.item",
                        reward.item(), reward.amount()).getString();
            case "acting":
                return "\u2022 " + Component.translatable("gui.guimi_mod.quest.reward.acting",
                        reward.amount()).getString();
            case "spirituality":
                return "\u2022 " + Component.translatable("gui.guimi_mod.quest.reward.spirituality",
                        reward.amount()).getString();
            default:
                return "\u2022 " + reward.type();
        }
    }

    private void drawButton(GuiGraphics graphics, int mouseX, int mouseY,
                            int x, int y, int w, Component label, String action, boolean enabled) {
        boolean hover = enabled && isInBox(mouseX, mouseY, x, y, w, 16);
        graphics.fill(x, y, x + w, y + 16,
                hover ? 0x88FFD700 : (enabled ? 0x66444444 : 0x33222222));
        graphics.drawCenteredString(this.font, label, x + w / 2, y + 4,
                hover ? 0x000000 : (enabled ? 0xFFFFFF : 0x666666));
    }

    /** 底部操作按钮（面板内水平居中）。 */
    private int[] buttonRects() {
        int bw = 78;
        int bx = (PANEL_W - (bw * 3 + 24)) / 2;
        return new int[] { bx, BTN_Y, bw };
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.minecraft == null || this.minecraft.player == null) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        computeLayout();
        Player player = this.minecraft.player;
        QuestData data = player.getData(ModAttachments.QUEST_DATA);
        int lx = toLocalX(mouseX);
        int ly = toLocalY(mouseY);

        if (mouseClickedColumn(player, data, mainTasks(player), true, lx, ly)) {
            return true;
        }
        if (mouseClickedColumn(player, data, sideTasks(player), false, lx, ly)) {
            return true;
        }

        // 底部操作按钮
        Task selTask = this.selected == null ? null : QuestClientState.get(this.selected);
        if (selTask != null) {
            boolean done = data.hasCompleted(selTask.id().toString());
            boolean active = data.hasActive(selTask.id().toString());
            int[] r = buttonRects();
            int bx = r[0];
            int btnY = r[1];
            int bw = r[2];

            if (!done && !active
                    && isInBox(lx, ly, bx, btnY, bw, 16)
                    && canAccept(player, data, selTask)) {
                PacketDistributor.sendToServer(new QuestActionPacket("accept", selTask.id().toString()));
                return true;
            }
            if (active && selTask.canAbandon()
                    && isInBox(lx, ly, bx + bw + 12, btnY, bw, 16)) {
                PacketDistributor.sendToServer(new QuestActionPacket("abandon", selTask.id().toString()));
                return true;
            }
            if (active
                    && isInBox(lx, ly, bx + 2 * (bw + 12), btnY, bw, 16)) {
                PacketDistributor.sendToServer(new QuestActionPacket("track", selTask.id().toString()));
                return true;
            }
        }

        // 捕获面板内点击，防止误操作底层
        if (isInBox(lx, ly, 0, 0, PANEL_W, PANEL_H)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean mouseClickedColumn(Player player, QuestData data, List<Task> list,
                                       boolean main, int lx, int ly) {
        int x = main ? COL_X1 : COL_X2;
        for (int i = 0; i < LIST_VISIBLE; i++) {
            int idx = i + (main ? this.scrollMain : this.scrollSide);
            if (idx >= list.size()) {
                break;
            }
            int y = LIST_TOP + i * ROW_H;
            if (isInBox(lx, ly, x, y, COL_W, ROW_H)) {
                this.selected = list.get(idx).id().toString();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        computeLayout();
        int lx = toLocalX(mouseX);
        int ly = toLocalY(mouseY);
        if (isInBox(lx, ly, COL_X1, LIST_TOP, COL_W,
                LIST_VISIBLE * ROW_H)) {
            int maxScroll = Math.max(0, mainTasks(this.minecraft.player).size() - LIST_VISIBLE);
            this.scrollMain = Math.max(0, Math.min(maxScroll,
                    this.scrollMain + (verticalAmount > 0 ? -1 : 1)));
            return true;
        }
        if (isInBox(lx, ly, COL_X2, LIST_TOP, COL_W,
                LIST_VISIBLE * ROW_H)) {
            int maxScroll = Math.max(0, sideTasks(this.minecraft.player).size() - LIST_VISIBLE);
            this.scrollSide = Math.max(0, Math.min(maxScroll,
                    this.scrollSide + (verticalAmount > 0 ? -1 : 1)));
            return true;
        }
        if (isInBox(lx, ly, 8, DETAIL_Y - 3,
                PANEL_W - 16, DETAIL_BOTTOM - DETAIL_Y + 3)) {
            this.scrollDetail += verticalAmount > 0 ? -1 : 1;
            if (this.scrollDetail < 0) {
                this.scrollDetail = 0;
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private static boolean isInBox(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
