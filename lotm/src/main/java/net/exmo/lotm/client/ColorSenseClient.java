package net.exmo.lotm.client;

import net.exmo.lotm.client.sequence.ClientSequenceState;
import net.exmo.lotm.PainterPassives;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.InfestedBlock;
import net.minecraft.world.level.block.SpawnerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.List;

/** Outlines nearby spawners and infested blocks, and names the nearest one. */
public final class ColorSenseClient {
    private static final int HORIZONTAL = 16;
    private static final int VERTICAL = 8;
    private static List<BlockPos> marks = List.of();
    private static long nextScan;

    private ColorSenseClient() {}

    public static boolean active() {
        var snapshot = ClientSequenceState.snapshot();
        if (snapshot == null || !snapshot.hasSequence()) return false;
        String id = PainterPassives.COLOR_SENSE.toString();
        for (var entry : snapshot.entries()) {
            for (var skill : entry.skills()) {
                if (id.equals(skill.id())) return true;
            }
        }
        return false;
    }

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || !active()) {
            marks = List.of();
            return;
        }
        long time = minecraft.level.getGameTime();
        if (time >= nextScan) {
            nextScan = time + 15L;
            marks = scan(minecraft);
        }
        if (marks.isEmpty()) return;
        Vec3 camera = event.getCamera().getPosition();
        var pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        var consumer = minecraft.renderBuffers().bufferSource().getBuffer(RenderType.lines());
        for (BlockPos pos : marks) {
            LevelRenderer.renderLineBox(pose, consumer, pos.getX(), pos.getY(), pos.getZ(),
                    pos.getX() + 1.0, pos.getY() + 1.0, pos.getZ() + 1.0, 0.78F, 0.28F, 0.95F, 1.0F);
        }
        minecraft.renderBuffers().bufferSource().endBatch(RenderType.lines());
        pose.popPose();
    }

    public static void hud(RenderGuiEvent.Post event) {
        if (!active() || marks.isEmpty()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.font == null) return;
        BlockPos nearest = marks.get(0);
        double best = Double.MAX_VALUE;
        BlockPos feet = minecraft.player.blockPosition();
        for (BlockPos pos : marks) {
            double distance = pos.distSqr(feet);
            if (distance < best) {
                best = distance;
                nearest = pos;
            }
        }
        int dx = nearest.getX() - feet.getX();
        int dz = nearest.getZ() - feet.getZ();
        String direction = Math.abs(dx) > Math.abs(dz)
                ? (dx > 0 ? "direction.lotm.east" : "direction.lotm.west")
                : (dz > 0 ? "direction.lotm.south" : "direction.lotm.north");
        int blocks = Math.max(1, (int) Math.round(Math.sqrt(best)));
        Component line = Component.translatable("passive.lotm.color_sense.near", marks.size(), Component.translatable(direction), blocks);
        var graphics = event.getGuiGraphics();
        int width = minecraft.font.width(line);
        graphics.drawString(minecraft.font, line, (graphics.guiWidth() - width) / 2, 6, 0xE7A6FF, true);
    }

    private static List<BlockPos> scan(Minecraft minecraft) {
        BlockPos center = minecraft.player.blockPosition();
        List<BlockPos> found = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        var level = minecraft.level;
        for (int dx = -HORIZONTAL; dx <= HORIZONTAL; dx++) {
            for (int dy = -VERTICAL; dy <= VERTICAL; dy++) {
                for (int dz = -HORIZONTAL; dz <= HORIZONTAL; dz++) {
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (!level.hasChunkAt(cursor)) continue;
                    BlockState state = level.getBlockState(cursor);
                    if (!disguised(state)) continue;
                    found.add(cursor.immutable());
                    if (found.size() >= 32) return found;
                }
            }
        }
        return found;
    }

    private static boolean disguised(BlockState state) {
        var block = state.getBlock();
        return block instanceof SpawnerBlock
                || block instanceof net.minecraft.world.level.block.TrialSpawnerBlock
                || block instanceof InfestedBlock;
    }
}
