package net.exmo.exworld.client.quest;

import net.exmo.exworld.client.battle.BattleClient;
import net.exmo.exworld.progress.QuestObjective;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/** Direction-and-distance plaque for coordinate objectives. It deliberately never moves the player. */
public final class QuestNavigationRenderer {
    private static final int WIDTH = 108;
    private static final int HEIGHT = 28;

    private QuestNavigationRenderer() {}

    public static void registerLayer(RegisterGuiLayersEvent event) {
        event.registerAboveAll(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("exworld", "quest_navigation"), (graphics, partial) -> render(graphics));
    }

    private static void render(GuiGraphics graphics) {
        if (BattleClient.active()) return;
        var minecraft = Minecraft.getInstance();
        var quest = QuestClient.navigation().orElse(null);
        if (quest == null || quest.objectives().isEmpty() || minecraft.player == null) return;
        var objective = quest.objectives().stream().filter(value -> value.type() == QuestObjective.Type.LOCATION).findFirst().orElse(null);
        if (objective == null) return;
        int centerX = graphics.guiWidth() / 2;
        int x = centerX - WIDTH / 2;
        int y = 8;
        JournalGuiTextures.panel(graphics, x, y, WIDTH, HEIGHT);
        if (!objective.dimension().isBlank() && !objective.dimension().equals(minecraft.player.level().dimension().location().toString())) {
            graphics.drawCenteredString(minecraft.font, Component.translatable("hud.exworld.quest_other_dimension"), centerX, y + 10, 0xFFFFE1A0);
            return;
        }
        double dx = objective.x() - minecraft.player.getX();
        double dz = objective.z() - minecraft.player.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        float desired = (float) (Math.atan2(dz, dx) * 180 / Math.PI) - 90;
        float delta = Mth.wrapDegrees(desired - minecraft.player.getYRot());
        String arrow = delta < -35 ? "◀" : delta > 35 ? "▶" : "▲";
        graphics.drawCenteredString(minecraft.font, arrow + " " + Math.round(distance) + "m", centerX, y + 10, 0xFFFFE1A0);
    }
}
