package net.exmo.lotm.client;

import net.exmo.lotm.client.sequence.ClientSequenceState;
import net.exmo.lotm.ApprenticePassives;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.List;

/** Outlines nearby portals and ender-pearl landing points for Spirit World Sense. */
public final class SpiritSenseClient {
    private static final int HORIZONTAL = 16;
    private static final int VERTICAL = 8;
    private static List<BlockPos> marks = List.of();
    private static int portals;
    private static int pearls;
    private static long nextScan;

    private SpiritSenseClient() {}

    public static boolean active() {
        var snapshot = ClientSequenceState.snapshot();
        if (snapshot == null || !snapshot.hasSequence()) return false;
        String id = ApprenticePassives.SPIRIT.toString();
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
            scan(minecraft);
        }
        if (marks.isEmpty()) return;
        Vec3 camera = event.getCamera().getPosition();
        var pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        var consumer = minecraft.renderBuffers().bufferSource().getBuffer(RenderType.lines());
        for (BlockPos pos : marks) {
            LevelRenderer.renderLineBox(pose, consumer, pos.getX(), pos.getY(), pos.getZ(),
                    pos.getX() + 1.0, pos.getY() + 1.0, pos.getZ() + 1.0, 0.35F, 0.85F, 1.0F, 1.0F);
        }
        minecraft.renderBuffers().bufferSource().endBatch(RenderType.lines());
        pose.popPose();
    }

    public static void hud(RenderGuiEvent.Post event) {
        if (!active() || (portals == 0 && pearls == 0)) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.font == null) return;
        Component line = Component.translatable("passive.lotm.spirit_sense.near", portals, pearls);
        var graphics = event.getGuiGraphics();
        int width = minecraft.font.width(line);
        graphics.drawString(minecraft.font, line, (graphics.guiWidth() - width) / 2, 18, 0x8EDBFF, true);
    }

    private static void scan(Minecraft minecraft) {
        List<BlockPos> found = new ArrayList<>();
        portals = 0;
        pearls = 0;
        BlockPos center = minecraft.player.blockPosition();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        var level = minecraft.level;
        for (int dx = -HORIZONTAL; dx <= HORIZONTAL; dx++) {
            for (int dy = -VERTICAL; dy <= VERTICAL; dy++) {
                for (int dz = -HORIZONTAL; dz <= HORIZONTAL; dz++) {
                    if (dx * dx + dy * dy + dz * dz > HORIZONTAL * HORIZONTAL) continue;
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (!level.hasChunkAt(cursor)) continue;
                    BlockState state = level.getBlockState(cursor);
                    if (!portal(state)) continue;
                    found.add(cursor.immutable());
                    portals++;
                    if (found.size() >= 48) break;
                }
            }
        }
        var pearlsInRange = level.getEntitiesOfClass(ThrownEnderpearl.class, minecraft.player.getBoundingBox().inflate(16.0),
                pearl -> pearl.distanceToSqr(minecraft.player) <= 16.0 * 16.0);
        for (ThrownEnderpearl pearl : pearlsInRange) {
            BlockPos landing = landing(pearl);
            if (landing != null) found.add(landing);
            pearls++;
        }
        marks = found;
    }

    private static boolean portal(BlockState state) {
        var block = state.getBlock();
        return block == Blocks.NETHER_PORTAL || block == Blocks.END_PORTAL || block == Blocks.END_GATEWAY;
    }

    private static BlockPos landing(ThrownEnderpearl pearl) {
        Vec3 pos = pearl.position();
        Vec3 velocity = pearl.getDeltaMovement();
        var level = pearl.level();
        for (int step = 0; step < 60; step++) {
            Vec3 next = pos.add(velocity);
            BlockHitResult hit = level.clip(new ClipContext(pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, pearl));
            if (hit.getType() != HitResult.Type.MISS) return hit.getBlockPos();
            pos = next;
            velocity = velocity.scale(0.99).add(0.0, -0.03, 0.0);
            if (pos.y < level.getMinBuildHeight()) return null;
        }
        return BlockPos.containing(pos);
    }
}
