package net.exmo.lotm.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.exmo.lotm.client.sequence.ClientSequenceState;
import net.exmo.lotm.SailorPassives;
import net.exmo.lotm.effect.LotmEffects;
import net.exmo.lotm.spell.SpiritVisionSpell;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.Tags;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

/** Local outlines for spirit vision, and predicted water speed for the sea blessing. */
public final class SpiritVisionClient {
    private static final int HORIZONTAL = 12;
    private static final int VERTICAL = 8;
    private static final RenderType LINES = RenderType.create(
            "lotm_spirit_lines",
            DefaultVertexFormat.POSITION_COLOR_NORMAL,
            VertexFormat.Mode.LINES,
            1536,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_LINES_SHADER)
                    .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.of(2.0)))
                    .setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setOutputState(RenderStateShard.ITEM_ENTITY_TARGET)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                    .createCompositeState(false));
    private static List<BlockPos> ores = List.of();
    private static List<BlockPos> traps = List.of();
    private static long nextScan;

    private SpiritVisionClient() {}

    public static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        SailorPassives.syncWater(minecraft.player, oceanAffinity());
    }

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || !minecraft.player.hasEffect(LotmEffects.SPIRIT_VISION)) {
            ores = List.of();
            traps = List.of();
            return;
        }
        long time = minecraft.level.getGameTime();
        if (time >= nextScan) {
            nextScan = time + 10L;
            scan(minecraft);
        }
        Vec3 camera = event.getCamera().getPosition();
        var pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        var consumer = minecraft.renderBuffers().bufferSource().getBuffer(LINES);
        for (BlockPos pos : ores) {
            LevelRenderer.renderLineBox(pose, consumer, pos.getX(), pos.getY(), pos.getZ(),
                    pos.getX() + 1.0, pos.getY() + 1.0, pos.getZ() + 1.0, 0.25F, 0.85F, 1.0F, 0.9F);
        }
        for (BlockPos pos : traps) {
            LevelRenderer.renderLineBox(pose, consumer, pos.getX() + 0.05, pos.getY() + 0.05, pos.getZ() + 0.05,
                    pos.getX() + 0.95, pos.getY() + 0.95, pos.getZ() + 0.95, 1.0F, 0.35F, 0.2F, 0.95F);
        }
        double range = SpiritVisionSpell.RANGE;
        AABB box = minecraft.player.getBoundingBox().inflate(range);
        for (LivingEntity living : minecraft.level.getEntitiesOfClass(LivingEntity.class, box,
                living -> living != minecraft.player && living.isAlive() && !(living instanceof ArmorStand))) {
            if (minecraft.player.distanceToSqr(living) > range * range) continue;
            AABB body = living.getBoundingBox().inflate(0.05);
            LevelRenderer.renderLineBox(pose, consumer, body, 0.75F, 0.45F, 1.0F, 0.9F);
        }
        minecraft.renderBuffers().bufferSource().endBatch(LINES);
        pose.popPose();
    }

    private static void scan(Minecraft minecraft) {
        List<BlockPos> foundOres = new ArrayList<>();
        List<BlockPos> foundTraps = new ArrayList<>();
        BlockPos center = minecraft.player.blockPosition();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        var level = minecraft.level;
        for (int dx = -HORIZONTAL; dx <= HORIZONTAL && (foundOres.size() < 64 || foundTraps.size() < 32); dx++) {
            for (int dy = -VERTICAL; dy <= VERTICAL && (foundOres.size() < 64 || foundTraps.size() < 32); dy++) {
                for (int dz = -HORIZONTAL; dz <= HORIZONTAL; dz++) {
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (!level.hasChunkAt(cursor)) continue;
                    BlockState state = level.getBlockState(cursor);
                    if (state.isAir()) continue;
                    if (foundOres.size() < 64 && state.is(Tags.Blocks.ORES)) {
                        foundOres.add(cursor.immutable());
                    } else if (foundTraps.size() < 32 && trap(state)) {
                        foundTraps.add(cursor.immutable());
                    }
                }
            }
        }
        ores = foundOres;
        traps = foundTraps;
    }

    private static boolean trap(BlockState state) {
        return state.getBlock() instanceof BasePressurePlateBlock
                || state.is(Blocks.TRIPWIRE)
                || state.is(Blocks.TRIPWIRE_HOOK)
                || state.is(Blocks.TNT)
                || state.is(Blocks.TRAPPED_CHEST);
    }

    private static boolean oceanAffinity() {
        var snapshot = ClientSequenceState.snapshot();
        if (snapshot == null || !snapshot.hasSequence()) return false;
        String id = SailorPassives.OCEAN.toString();
        for (var entry : snapshot.entries()) {
            for (var skill : entry.skills()) {
                if (id.equals(skill.id())) return true;
            }
        }
        return false;
    }
}
