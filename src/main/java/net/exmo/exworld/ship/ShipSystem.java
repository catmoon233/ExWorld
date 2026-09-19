package net.exmo.exworld.ship;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.exmo.exworld.content.ExWorldContent;
import net.exmo.exworld.network.ShipNetwork;
import net.exmo.exworld.ship.assembly.ShipCapture;
import net.exmo.exworld.ship.assembly.ShipMaterializer;
import net.exmo.exworld.ship.entity.ShipEntity;
import net.exmo.exworld.ship.interact.ShipInteractions;
import net.exmo.exworld.ship.model.PartSelection;
import net.exmo.exworld.ship.model.ShipHull;
import net.exmo.exworld.ship.model.ShipTemplate;
import net.exmo.exworld.ship.storage.ShipNbtCodec;
import net.exmo.exworld.ship.storage.ShipTemplates;
import net.exmo.exworld.ship.storage.ShipTemplateStore;
import net.exmo.exworld.ship.upgrade.ShipUpgradeRules;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Commands, tool capture and 实体化. Permission 2 for administration. */
public final class ShipSystem {
    private static final Map<UUID, Selection> SELECTIONS = new HashMap<>();

    private ShipSystem() {}

    public static void registerEvents() { NeoForge.EVENT_BUS.register(ShipSystem.class); }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("exworld").then(Commands.literal("ship").requires(source -> source.hasPermission(2))
                .then(Commands.literal("tool").executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    player.addItem(new ItemStack(ExWorldContent.SHIP_TOOL.get()));
                    player.addItem(new ItemStack(ExWorldContent.SHIP_CORE.get()));
                    player.addItem(new ItemStack(ExWorldContent.SHIP_HELM.get()));
                    context.getSource().sendSuccess(() -> Component.translatable("command.exworld.ship.tool"), false);
                    return 1;
                }))
                .then(Commands.literal("list").executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    List<ShipTemplate> templates = ShipTemplateStore.get(player.getServer()).all();
                    if (templates.isEmpty()) context.getSource().sendSuccess(() -> Component.translatable("command.exworld.ship.empty"), false);
                    else templates.forEach(template -> context.getSource().sendSuccess(() ->
                            Component.literal(template.id() + " · " + template.name() + " · " + template.hull().size()), false));
                    return templates.size();
                }))
                .then(Commands.literal("temple").executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    ShipTemplateStore.get(player.getServer()).put(ShipTemplates.temple());
                    openEditor(player, ShipTemplates.TEMPLE_ID);
                    return 1;
                }))
                .then(Commands.literal("edit").executes(context -> {
                    openEditor(context.getSource().getPlayerOrException(), "");
                    return 1;
                }).then(Commands.argument("id", StringArgumentType.word()).executes(context -> {
                    openEditor(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "id"));
                    return 1;
                })))
                .then(Commands.literal("spawn").then(Commands.argument("id", StringArgumentType.word()).executes(context -> {
                    spawn(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "id"));
                    return 1;
                })))
                .then(Commands.literal("disassemble").executes(context -> {
                    disassembleLooked(context.getSource().getPlayerOrException());
                    return 1;
                }))
        ));
    }

    @SubscribeEvent
    public static void leftClick(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!player.getMainHandItem().is(ExWorldContent.SHIP_TOOL.get())) return;
        if (!player.hasPermissions(2)) return;
        useTool(player, event.getPos(), false, false);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void tracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ShipEntity ship && event.getEntity() instanceof ServerPlayer player) {
            ShipNetwork.sendHull(player, ship);
        }
    }

    public static void useTool(ServerPlayer player, BlockPos pos, boolean shift, boolean right) {
        if (!player.hasPermissions(2)) {
            player.sendSystemMessage(Component.translatable("command.exworld.ship.denied"));
            return;
        }
        Selection selection = SELECTIONS.computeIfAbsent(player.getUUID(), id -> new Selection());
        if (shift && right) {
            try {
                ShipCapture.Result captured = ShipCapture.flood(player.serverLevel(), pos);
                selection.floodOrigin = pos.immutable();
                selection.a = captured.origin();
                selection.b = captured.origin().offset(
                        Math.max(0, captured.hull().sizeX() - 1),
                        Math.max(0, captured.hull().sizeY() - 1),
                        Math.max(0, captured.hull().sizeZ() - 1));
                player.sendSystemMessage(Component.translatable("message.exworld.ship.flood", captured.hull().size()));
                ShipNetwork.sendOverlay(player, selection.a, selection.b, true);
            } catch (IllegalArgumentException error) {
                player.sendSystemMessage(Component.literal(error.getMessage()));
            }
            return;
        }
        if (!right) selection.a = pos.immutable();
        else selection.b = pos.immutable();
        selection.floodOrigin = null;
        if (selection.a != null && selection.b != null) ShipNetwork.sendOverlay(player, selection.a, selection.b, true);
        player.sendSystemMessage(Component.translatable(right ? "message.exworld.ship.corner2" : "message.exworld.ship.corner1",
                pos.getX(), pos.getY(), pos.getZ()));
    }

    public static void openEditor(ServerPlayer player, String id) {
        if (!player.hasPermissions(2)) return;
        ShipTemplateStore store = ShipTemplateStore.get(player.getServer());
        ShipTemplate current = id.isBlank() ? store.all().stream().findFirst().orElse(blankTemplate()) : store.get(id).orElse(blankTemplate().withId(id));
        List<String> ids = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (ShipTemplate template : store.all()) { ids.add(template.id()); names.add(template.name()); }
        ShipNetwork.sendEditor(player, current.id(), ShipNbtCodec.encodeTemplate(current), ids, names);
    }

    public static void saveTemplate(ServerPlayer player, byte[] bytes) {
        if (!player.hasPermissions(2)) return;
        try {
            ShipTemplate template = ShipNbtCodec.decodeTemplate(bytes);
            for (var part : template.parts()) if (part.empty()) {
                player.sendSystemMessage(Component.translatable("message.exworld.ship.empty_part", part.id()));
                return;
            }
            ShipTemplateStore.get(player.getServer()).put(template);
            player.sendSystemMessage(Component.translatable("message.exworld.ship.saved", template.id()));
        } catch (RuntimeException error) {
            player.sendSystemMessage(Component.literal(error.getMessage()));
        }
    }

    public static void action(ServerPlayer player, String action, String id, int entityId) {
        switch (action) {
            case "OPEN_EDITOR" -> openEditor(player, id);
            case "CAPTURE" -> capture(player, id);
            case "MATERIALIZE" -> materialize(player, id);
            case "DISASSEMBLE" -> { if (player.hasPermissions(2)) disassembleLooked(player); }
            case "SPAWN" -> { if (player.hasPermissions(2)) spawn(player, id); }
            case "OPEN_UPGRADE" -> openUpgrade(player, entityId);
            default -> {}
        }
    }

    public static void interact(ServerPlayer player, int entityId, int x, int y, int z) {
        Entity entity = player.serverLevel().getEntity(entityId);
        if (!(entity instanceof ShipEntity ship)) return;
        if (!ShipInteractions.inReach(player, ship, x, y, z)) return;
        ShipInteractions.handle(player, ship, x, y, z);
    }

    public static void drive(ServerPlayer player, int entityId, int flags, float yaw) {
        Entity entity = player.serverLevel().getEntity(entityId);
        if (entity instanceof ShipEntity ship && player.getVehicle() == ship) ship.drive(flags, yaw);
    }

    public static void upgrade(ServerPlayer player, int entityId, String partId, String variantId) {
        Entity entity = player.serverLevel().getEntity(entityId);
        if (!(entity instanceof ShipEntity ship)) return;
        if (!near(player, ship)) return;
        ShipTemplateStore store = ShipTemplateStore.get(player.getServer());
        ShipTemplate template = store.get(ship.templateId()).orElse(null);
        if (template == null) {
            player.sendSystemMessage(Component.translatable("message.exworld.ship.unknown", ship.templateId()));
            return;
        }
        template = template.withHull(ship.hull()).withSelection(ship.selection());
        ShipTemplate variant = store.get(variantId).orElse(null);
        if (variant == null) {
            player.sendSystemMessage(Component.translatable("message.exworld.ship.unknown", variantId));
            return;
        }
        var result = ShipUpgradeRules.apply(template, partId, variantId, variant.hull());
        if (!result.ok()) {
            player.sendSystemMessage(Component.literal(result.error()));
            return;
        }
        ship.replaceHull(result.template().hull(), result.template().selection());
        player.sendSystemMessage(Component.translatable("message.exworld.ship.upgraded", partId, variantId));
    }

    private static void capture(ServerPlayer player, String id) {
        if (!player.hasPermissions(2)) return;
        Selection selection = SELECTIONS.get(player.getUUID());
        if (selection == null || selection.a == null || selection.b == null) {
            player.sendSystemMessage(Component.translatable("message.exworld.ship.no_selection"));
            return;
        }
        try {
            ShipCapture.Result captured = selection.floodOrigin != null
                    ? ShipCapture.flood(player.serverLevel(), selection.floodOrigin)
                    : ShipCapture.box(player.serverLevel(), selection.a, selection.b);
            String templateId = id == null || id.isBlank() ? "ship_" + Integer.toHexString(player.tickCount) : id;
            ShipTemplate template = new ShipTemplate(templateId, templateId, captured.hull(), List.of(), PartSelection.empty());
            ShipTemplateStore.get(player.getServer()).put(template);
            player.sendSystemMessage(Component.translatable("message.exworld.ship.captured", templateId, captured.hull().size()));
            openEditor(player, templateId);
        } catch (IllegalArgumentException error) {
            player.sendSystemMessage(Component.literal(error.getMessage()));
        }
    }

    private static void materialize(ServerPlayer player, String id) {
        if (!player.hasPermissions(2)) return;
        Selection selection = SELECTIONS.get(player.getUUID());
        try {
            if (selection == null || selection.a == null || selection.b == null) {
                player.sendSystemMessage(Component.translatable("message.exworld.ship.no_selection"));
                return;
            }
            ShipCapture.Result captured = selection.floodOrigin != null
                    ? ShipCapture.flood(player.serverLevel(), selection.floodOrigin)
                    : ShipCapture.box(player.serverLevel(), selection.a, selection.b);
            ShipHull hull = captured.hull();
            BlockPos origin = captured.origin();
            ShipTemplate stored = id == null || id.isBlank() ? null : ShipTemplateStore.get(player.getServer()).get(id).orElse(null);
            String templateId = stored == null ? "lifted" : stored.id();
            PartSelection partSelection = stored == null ? PartSelection.empty() : stored.selection();
            double speed = stored == null ? 0.35 : stored.maxSpeed();
            ShipMaterializer.lift(player.serverLevel(), hull, templateId, partSelection, speed, Vec3.atLowerCornerOf(origin));
            ShipMaterializer.clearWorld(player.serverLevel(), origin, hull);
            player.sendSystemMessage(Component.translatable("message.exworld.ship.materialized", hull.size()));
        } catch (RuntimeException error) {
            player.sendSystemMessage(Component.literal(error.getMessage()));
        }
    }

    private static void spawn(ServerPlayer player, String id) {
        ShipTemplate template = ShipTemplateStore.get(player.getServer()).get(id).orElse(null);
        if (template == null) {
            player.sendSystemMessage(Component.translatable("message.exworld.ship.unknown", id));
            return;
        }
        Vec3 look = player.position().add(player.getLookAngle().scale(4));
        BlockPos origin = BlockPos.containing(look);
        if (!ShipMaterializer.destinationClear(player.serverLevel(), origin, template.hull())) {
            player.sendSystemMessage(Component.translatable("message.exworld.ship.blocked"));
            return;
        }
        ShipMaterializer.spawn(player.serverLevel(), template, Vec3.atLowerCornerOf(origin));
        player.sendSystemMessage(Component.translatable("message.exworld.ship.spawned", id));
    }

    private static void disassembleLooked(ServerPlayer player) {
        Entity hit = player.serverLevel().getEntities(player, player.getBoundingBox().expandTowards(player.getLookAngle().scale(8)).inflate(1),
                entity -> entity instanceof ShipEntity).stream().findFirst().orElse(null);
        if (!(hit instanceof ShipEntity ship)) {
            player.sendSystemMessage(Component.translatable("message.exworld.ship.no_ship"));
            return;
        }
        try {
            ShipMaterializer.disassemble(player.serverLevel(), ship);
            player.sendSystemMessage(Component.translatable("message.exworld.ship.disassembled"));
        } catch (RuntimeException error) {
            player.sendSystemMessage(Component.literal(error.getMessage()));
        }
    }

    private static void openUpgrade(ServerPlayer player, int entityId) {
        Entity entity = player.serverLevel().getEntity(entityId);
        if (!(entity instanceof ShipEntity ship)) return;
        if (!near(player, ship)) return;
        ShipTemplateStore store = ShipTemplateStore.get(player.getServer());
        ShipTemplate template = store.get(ship.templateId()).orElse(new ShipTemplate(ship.templateId(), ship.templateId(),
                ship.hull(), List.of(), ship.selection()));
        template = template.withHull(ship.hull()).withSelection(ship.selection());
        List<String> ids = new ArrayList<>();
        List<byte[]> hulls = new ArrayList<>();
        for (var part : template.parts()) {
            for (String variantId : part.allowedVariants()) {
                store.get(variantId).ifPresent(variant -> {
                    if (!ids.contains(variant.id())) {
                        ids.add(variant.id());
                        hulls.add(ShipNbtCodec.encodeHull(variant.hull()));
                    }
                });
            }
        }
        ShipNetwork.sendUpgradeScreen(player, ship.getId(), ShipNbtCodec.encodeTemplate(template), ids, hulls);
    }

    private static ShipTemplate blankTemplate() {
        return new ShipTemplate("new_ship", "新飞船", ShipHull.empty(), List.of(), PartSelection.empty());
    }

    private static boolean near(ServerPlayer player, ShipEntity ship) {
        return ship.getBoundingBox().inflate(8).contains(player.getEyePosition());
    }

    private static final class Selection {
        BlockPos a;
        BlockPos b;
        BlockPos floodOrigin;
    }
}
