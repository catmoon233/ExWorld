package net.exmo.exworld.client.ship;

import net.exmo.exworld.content.ExWorldContent;
import net.exmo.exworld.network.ShipNetwork;
import net.exmo.exworld.ship.entity.ShipEntity;
import net.exmo.exworld.ship.interact.ShipRaycast;
import net.exmo.exworld.ship.model.PartSelection;
import net.exmo.exworld.ship.motion.AboardAttachment;
import net.exmo.exworld.ship.storage.ShipNbtCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;
import java.util.Map;

/** Client hub for ship screens, overlays, driving input and hull sync. */
public final class ShipClient {
    private ShipClient() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(ShipClient::renderers);
        NeoForge.EVENT_BUS.addListener(ShipToolOverlay::render);
        NeoForge.EVENT_BUS.addListener(ShipClient::tick);
        NeoForge.EVENT_BUS.addListener(ShipClient::click);
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ExWorldContent.SHIP.get(), ShipRenderer::new);
    }

    public static void openEditor(String currentId, byte[] template, List<String> ids, List<String> names) {
        Minecraft.getInstance().setScreen(new ShipEditorScreen(currentId, template, ids, names));
    }

    public static void openUpgrade(int entityId, byte[] template, List<String> variantIds, List<byte[]> variantHulls) {
        Minecraft.getInstance().setScreen(new ShipUpgradeScreen(entityId, template, variantIds, variantHulls));
    }

    public static void overlay(BlockPos a, BlockPos b, boolean present) {
        ShipToolOverlay.install(a, b, present);
    }

    public static void installHull(int entityId, byte[] hull, String templateId, Map<String, String> selection, double speed) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        Entity entity = minecraft.level.getEntity(entityId);
        if (entity instanceof ShipEntity ship) {
            ship.clientInstall(ShipNbtCodec.decodeHull(hull), templateId, new PartSelection(selection), speed);
        }
    }

    public static void applyDelta(int entityId, int x, int y, int z, String block) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        Entity entity = minecraft.level.getEntity(entityId);
        if (entity instanceof ShipEntity ship) ship.setBlock(x, y, z, block);
    }

    private static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof LocalPlayer player)) return;
        if (player.getVehicle() instanceof ShipEntity ship) {
            int flags = 0;
            var options = Minecraft.getInstance().options;
            if (options.keyUp.isDown()) flags |= 1;
            if (options.keyDown.isDown()) flags |= 2;
            if (options.keyLeft.isDown()) flags |= 4;
            if (options.keyRight.isDown()) flags |= 8;
            if (options.keyJump.isDown()) flags |= 16;
            if (options.keySprint.isDown()) flags |= 32;
            ShipNetwork.drive(ship.getId(), flags, player.getYRot());
            return;
        }
        snapAboard(player);
    }

    private static void snapAboard(LocalPlayer player) {
        for (Entity entity : player.level().getEntities(player, player.getBoundingBox().inflate(4))) {
            if (!(entity instanceof ShipEntity ship)) continue;
            Vec3 carried = player.position().add(ship.lastDelta());
            Vec3 local = ship.toLocal(carried);
            AboardAttachment.Result result = AboardAttachment.inspect(local.x, local.y, local.z, ship.hull());
            if (!result.aboard()) continue;
            Vec3 snapped = ship.toWorld(new Vec3(local.x, result.snapLocalY(), local.z));
            if (Math.abs(carried.y - snapped.y) < 1.5) {
                player.setPos(snapped.x, snapped.y, snapped.z);
                player.setDeltaMovement(player.getDeltaMovement().multiply(1, 0, 1));
                player.setOnGround(true);
            }
            return;
        }
    }

    private static void click(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) return;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.screen != null) return;
        HitResult hit = minecraft.hitResult;
        ShipEntity ship = null;
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof ShipEntity found) ship = found;
        if (ship == null) return;
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1);
        Vec3 localOrigin = ship.toLocal(eye);
        Vec3 localLook = ship.toLocal(eye.add(look)).subtract(localOrigin);
        ShipRaycast.Occupied occupied = ship.hull()::occupied;
        var traced = ShipRaycast.trace(localOrigin.x, localOrigin.y, localOrigin.z, localLook.x, localLook.y, localLook.z, 8, occupied);
        if (traced.isEmpty()) return;
        event.setCanceled(true);
        event.setSwingHand(true);
        var voxel = traced.get();
        if (player.isShiftKeyDown()) ShipNetwork.action("OPEN_UPGRADE", "", ship.getId());
        else ShipNetwork.interact(ship.getId(), voxel.x(), voxel.y(), voxel.z());
    }
}
