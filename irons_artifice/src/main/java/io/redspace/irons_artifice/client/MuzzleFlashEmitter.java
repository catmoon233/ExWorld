package io.redspace.irons_artifice.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.redspace.irons_artifice.IronsArtifice;
import io.redspace.irons_artifice.data.ParticleBurst;
import io.redspace.irons_artifice.network.packets.ClientboundMuzzleFlashPacket;
import io.redspace.irons_artifice.network.packets.MuzzleFlashVisuals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@EventBusSubscriber(modid = IronsArtifice.MODID, value = Dist.CLIENT)
public final class MuzzleFlashEmitter {
    private static final Map<Integer, ClientboundMuzzleFlashPacket> PENDING = new HashMap<>();
    private static final float BARREL_CLEARANCE = 0.55F;
    private static final double MIN_CAMERA_FORWARD_DISTANCE = 1.05;

    public static void enqueue(ClientboundMuzzleFlashPacket packet) {
        if (Minecraft.getInstance().level == null || Minecraft.getInstance().player == null) {
            return;
        }
        PENDING.put(packet.entityId(), packet);
    }

    public static void tryEmit(int entityId, PoseStack poseStack) {
        if (isShadowPass() || !PENDING.containsKey(entityId)) {
            return;
        }
        ClientboundMuzzleFlashPacket packet = PENDING.remove(entityId);
        if (packet == null) {
            return;
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        spawn(level, packet, worldPosFromBone(poseStack, packet.extraForwardOffset()));
    }

    private static boolean isShadowPass() {
        try {
            Class<?> state = Class.forName("net.irisshaders.iris.shadows.ShadowRenderingState");
            Object rendering = state.getMethod("areShadowsCurrentlyBeingRendered").invoke(null);
            return rendering instanceof Boolean shadow && shadow;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    @SubscribeEvent
    static void onRenderFrame(RenderFrameEvent.Post event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || PENDING.isEmpty() || Minecraft.getInstance().isPaused()) {
            return;
        }
        Iterator<ClientboundMuzzleFlashPacket> iterator = PENDING.values().iterator();
        while (iterator.hasNext()) {
            ClientboundMuzzleFlashPacket packet = iterator.next();
            spawn(level, packet, packet.backupPos());
            iterator.remove();
        }
    }

    private static Vec3 worldPosFromBone(PoseStack poseStack, float extraForwardOffset) {
        Vector3f origin = poseStack.last().pose().transformPosition(new Vector3f());
        Vector3f forward = poseStack.last().pose().transformDirection(new Vector3f(0f, 0f, -1f));
        if (forward.lengthSquared() > 1.0e-6f) {
            forward.normalize();
        }
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        float push = extraForwardOffset + BARREL_CLEARANCE;
        Vec3 relative = new Vec3(origin.x, origin.y, origin.z)
                .add(forward.x * push, forward.y * push, forward.z * push);
        Vector3f cameraForward = new Vector3f(Minecraft.getInstance().gameRenderer.getMainCamera().getLookVector());
        if (cameraForward.lengthSquared() > 1.0e-6f) {
            cameraForward.normalize();
            double forwardDistance = relative.x * cameraForward.x + relative.y * cameraForward.y + relative.z * cameraForward.z;
            if (forwardDistance < MIN_CAMERA_FORWARD_DISTANCE) {
                double correction = MIN_CAMERA_FORWARD_DISTANCE - forwardDistance;
                relative = relative.add(cameraForward.x * correction, cameraForward.y * correction, cameraForward.z * correction);
            }
        }
        return camera.add(relative);
    }

    private static void spawn(ClientLevel level, ClientboundMuzzleFlashPacket msg, Vec3 pos) {
        MuzzleFlashVisuals visuals = msg.visuals();
        if (level.isFluidAtPosition(BlockPos.containing(pos), s -> s.is(FluidTags.WATER))
                && !visuals.underwaterBursts().isEmpty()) {
            for (ParticleBurst burst : visuals.underwaterBursts()) {
                spawnBurst(level, burst, pos);
            }
            return;
        }
        visuals.flash().ifPresent(flash -> {
            Vec3 random = new Vec3(level.getRandom().nextDouble() - 0.5, level.getRandom().nextDouble() - 0.5, level.getRandom().nextDouble() - 0.5).scale(2).scale(0.02);
            Vec3 motion = msg.entityMotion().scale(0.5).add(random);
            level.addAlwaysVisibleParticle(flash, true, pos.x, pos.y, pos.z, motion.x, motion.y, motion.z);
        });
        for (ParticleBurst burst : visuals.airBursts()) {
            spawnBurst(level, burst, pos);
        }
    }

    private static void spawnBurst(ClientLevel level, ParticleBurst burst, Vec3 pos) {
        for (int i = 0; i < burst.count(); i++) {
            Vec3 motion = new Vec3(
                    level.getRandom().nextDouble() - 0.5,
                    level.getRandom().nextDouble() - 0.5,
                    level.getRandom().nextDouble() - 0.5
            ).scale(2).scale(burst.velocityScale());
            level.addAlwaysVisibleParticle(burst.particle(), burst.force(), pos.x, pos.y, pos.z, motion.x, motion.y, motion.z);
        }
    }
}
