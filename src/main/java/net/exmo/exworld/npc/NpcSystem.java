package net.exmo.exworld.npc;

import net.exmo.exworld.content.ExWorldContent;
import net.exmo.exworld.npc.data.ActionSpec;
import net.exmo.exworld.npc.data.DialogScript;
import net.exmo.exworld.npc.data.NpcCatalog;
import net.exmo.exworld.npc.data.NpcCodec;
import net.exmo.exworld.npc.data.NpcDocument;
import net.exmo.exworld.npc.data.NpcPlace;
import net.exmo.exworld.npc.data.NpcRoute;
import net.exmo.exworld.npc.data.NpcValidator;
import net.exmo.exworld.npc.data.RelationEdge;
import net.exmo.exworld.npc.data.TradeSpec;
import net.exmo.exworld.npc.dialog.DialogRequest;
import net.exmo.exworld.npc.dialog.HttpDialogModel;
import net.exmo.exworld.npc.entity.UrbanNpc;
import net.exmo.exworld.npc.item.NpcWandItem;
import net.exmo.exworld.npc.logic.RelationGraph;
import net.exmo.exworld.npc.network.NpcNetwork;
import net.neoforged.neoforge.network.PacketDistributor;
import net.exmo.exworld.npc.network.NpcPayloads;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
 import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

/** Server authority for documents, dialogue, trading and the editor wand. */
public final class NpcSystem {
    private static final HttpDialogModel DIALOG = new HttpDialogModel();

    private NpcSystem() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(NpcSystem::attributes);
        NeoForge.EVENT_BUS.addListener(NpcCommands::register);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(ExWorldContent.URBAN_NPC.get(), UrbanNpc.createAttributes().build());
    }

    public static AttributeSupplier unused() { return UrbanNpc.createAttributes().build(); }

    public static void interact(ServerPlayer player, UrbanNpc npc) {
        NpcCatalog catalog = NpcCatalog.get(player.server);
        NpcDocument doc = catalog.document(npc.documentId()).orElse(null);
        if (doc == null) return;
        if (canTrade(npc, doc) && !npc.getOffers().isEmpty()) {
            openTrade(player, npc);
            return;
        }
        String dialogId = doc.dialog("closed").isPresent() ? "closed" : doc.defaultDialogId();
        openDialog(player, npc, doc, catalog, dialogId);
    }

    public static void openDialog(ServerPlayer player, UrbanNpc npc, NpcDocument doc, NpcCatalog catalog, String dialogId) {
        DialogScript script = doc.dialog(dialogId).orElseGet(() -> doc.dialog(doc.defaultDialogId()).orElse(null));
        if (script == null) {
            player.displayClientMessage(Component.translatable("npc.exworld.no_dialog"), true);
            return;
        }
        npc.setDialogOpen(true);
        if (script.mode() == DialogScript.DialogMode.AI) {
            NpcNetwork.dialog(player, npc, script, "……", true);
            DialogRequest request = new DialogRequest(doc.displayName(), npc.nodeId(),
                    new RelationGraph(catalog.relations()).summary(doc.id()) + " " + script.prompt(),
                    player.getGameProfile().getName(), List.of());
            int entityId = npc.getId();
            DIALOG.request(request).whenComplete((text, error) -> player.server.execute(() -> {
                if (!player.isAlive()) return;
                Entity entity = player.serverLevel().getEntity(entityId);
                String line = error != null || text == null || text.isBlank() ? script.fallbackOrLine() : text.trim();
                if (entity instanceof UrbanNpc living) NpcNetwork.dialog(player, living, script, line, false);
            }));
            return;
        }
        NpcNetwork.dialog(player, npc, script, script.primaryLine(), false);
    }

    public static void openTrade(ServerPlayer player, UrbanNpc npc) {
        npc.setTradingPlayer(player);
        OptionalInt menu = player.openMenu(new SimpleMenuProvider(
                (id, inventory, unused) -> new MerchantMenu(id, inventory, npc), npc.getDisplayName()));
        menu.ifPresent(id -> player.sendMerchantOffers(id, npc.getOffers(), 1, npc.getVillagerXp(), false, false));
    }
    public static boolean canTrade(UrbanNpc npc, NpcDocument doc) {
        if (npc == null || doc == null || !npc.tradingOpen()) return false;
        String place = npc.tradePlaceId();
        if (place.isBlank()) place = tradePlace(doc, "", npc.nodeId());
        return npc.arrivedAt(doc, place);
    }

    public static boolean canTrade(UrbanNpc npc, NpcDocument doc, ActionSpec action) {
        if (action == null || !"trade".equals(action.type()) || !canTrade(npc, doc)) return false;
        String place = action.param("place", "");
        return place.isBlank() || npc.arrivedAt(doc, place);
    }

    private static String tradePlace(NpcDocument doc, String actionId, String nodeId) {
        if (actionId != null && !actionId.isBlank()) {
            String fromAction = doc.action(actionId).map(spec -> spec.param("place", "")).orElse("");
            if (!fromAction.isBlank()) return fromAction;
        }
        if (nodeId != null) {
            for (var node : doc.timeline()) {
                if (nodeId.equals(node.id()) && !node.placeId().isBlank()) return node.placeId();
            }
        }
        return doc.homePlaceId();
    }


    public static MerchantOffers offers(NpcDocument doc) {
        MerchantOffers offers = new MerchantOffers();
        for (TradeSpec trade : doc.trades()) {
            try {
                Item pay = BuiltInRegistries.ITEM.get(ResourceLocation.parse(trade.payItem()));
                Item result = BuiltInRegistries.ITEM.get(ResourceLocation.parse(trade.resultItem()));
                if (pay == Items.AIR || result == Items.AIR) continue;
                offers.add(new MerchantOffer(new ItemCost(pay, trade.payCount()),
                        new ItemStack(result, trade.resultCount()), trade.maxUses(), 0, 0.05f));
            } catch (RuntimeException ignored) {
            }
        }
        return offers;
    }

    public static void handle(ServerPlayer player, NpcPayloads.Server payload) {
        if (payload == null || payload.tag() == null) return;
        switch (payload.kind()) {
            case "save" -> save(player, payload.tag());
            case "dialog_button" -> button(player, payload.tag());
            case "dialog_close" -> close(player, payload.tag());
            case "tool" -> pushTool(player);
            default -> {}
        }
    }

    private static void save(ServerPlayer player, CompoundTag tag) {
        if (!player.hasPermissions(2)) {
            NpcNetwork.result(player, false, "permission");
            return;
        }
        try {
            NpcDocument document = NpcCodec.loadDocument(tag.getCompound("document"));
            List<RelationEdge> edges = new ArrayList<>();
            ListTag list = tag.getList("relations", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) edges.add(NpcCodec.loadEdge(list.getCompound(i)));
            String error = NpcValidator.validate(document, edges);
            if (error != null) {
                NpcNetwork.result(player, false, error);
                return;
            }
            NpcCatalog.get(player.server).put(document, edges);
            pushTool(player);
            NpcNetwork.result(player, true, "");
        } catch (RuntimeException ex) {
            NpcNetwork.result(player, false, "save failed");
        }
    }

    private static void button(ServerPlayer player, CompoundTag tag) {
        Entity entity = player.serverLevel().getEntity(tag.getInt("entity"));
        if (!(entity instanceof UrbanNpc npc)) return;
        NpcCatalog catalog = NpcCatalog.get(player.server);
        NpcDocument doc = catalog.document(npc.documentId()).orElse(null);
        if (doc == null) return;
        DialogScript script = doc.dialog(tag.getString("dialog")).orElse(null);
        int index = tag.getInt("button");
        if (script == null || index < 0 || index >= script.buttons().size()) return;
        DialogScript.DialogButton button = script.buttons().get(index);
        if (!button.dialogId().isBlank()) {
            openDialog(player, npc, doc, catalog, button.dialogId());
            return;
        }
        ActionSpec action = doc.action(button.actionId()).orElse(null);
        if (action != null && "trade".equals(action.type()) && canTrade(npc, doc, action)) openTrade(player, npc);
        else player.displayClientMessage(Component.translatable("npc.exworld.closed"), true);
    }

    private static void close(ServerPlayer player, CompoundTag tag) {
        Entity entity = player.serverLevel().getEntity(tag.getInt("entity"));
        if (entity instanceof UrbanNpc npc) npc.setDialogOpen(false);
    }

    public static InteractionResult wandOnEntity(ServerPlayer player, ItemStack stack, LivingEntity entity) {
        if (!player.hasPermissions(2)) return InteractionResult.FAIL;
        if (!(entity instanceof UrbanNpc npc)) return InteractionResult.PASS;
        NpcWandItem.remember(stack, npc.documentId());
        pushTool(player);
        openEditor(player, npc.documentId(), "");
        return InteractionResult.CONSUME;
    }

    public static void openBlueprint(ServerPlayer player, String documentId) {
        openView(player, documentId, "blueprint", "");
    }

    public static void openEditor(ServerPlayer player, String documentId, String error) {
        openView(player, documentId, "editor", error);
    }

    private static void openView(ServerPlayer player, String documentId, String kind, String error) {
        if (!player.hasPermissions(2)) {
            player.displayClientMessage(Component.translatable("npc.exworld.no_permission"), true);
            return;
        }
        NpcCatalog catalog = NpcCatalog.get(player.server);
        NpcDocument doc = catalog.document(documentId).orElse(null);
        if (doc == null) {
            player.displayClientMessage(Component.translatable("npc.exworld.missing"), true);
            return;
        }
        ListTag edges = new ListTag();
        for (RelationEdge edge : catalog.outgoing(documentId)) edges.add(NpcCodec.saveEdge(edge));
        CompoundTag tag = new CompoundTag();
        tag.put("document", NpcCodec.saveDocument(doc));
        tag.put("relations", edges);
        tag.putString("ids", String.join(",", catalog.ids()));
        tag.putString("error", error == null ? "" : error);
        PacketDistributor.sendToPlayer(player, new NpcPayloads.Client(kind, tag));
        pushTool(player);
    }

    public static void pushTool(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof NpcWandItem)) stack = player.getOffhandItem();
        if (!(stack.getItem() instanceof NpcWandItem) || !player.hasPermissions(2)) return;
        NpcCatalog catalog = NpcCatalog.get(player.server);
        NpcDocument doc = catalog.document(NpcWandItem.documentId(stack)).orElse(null);
        CompoundTag tag = new CompoundTag();
        if (doc == null) {
            tag.putString("id", "");
            PacketDistributor.sendToPlayer(player, new NpcPayloads.Client("overlay", tag));
            return;
        }
        String fallbackDim = doc.place(doc.homePlaceId()).map(NpcPlace::dimension).orElse("minecraft:overworld");
        tag.putString("id", doc.id());
        tag.putString("name", doc.displayName());
        tag.putInt("mode", NpcWandItem.mode(stack));
        ListTag places = new ListTag();
        for (NpcPlace place : doc.places()) {
            CompoundTag point = new CompoundTag();
            point.putString("id", place.id());
            point.putDouble("x", place.x());
            point.putDouble("y", place.y());
            point.putDouble("z", place.z());
            point.putString("dim", place.dimension());
            point.putDouble("r", place.radius());
            point.putString("role", place.id().equals(doc.homePlaceId()) ? "home" : "post".equals(place.id()) ? "post" : "place");
            places.add(point);
        }
        tag.put("places", places);
        ListTag routes = new ListTag();
        for (NpcRoute route : doc.routes()) {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", route.id());
            entry.putBoolean("active", route.id().equals(doc.activeRouteId()));
            ListTag points = new ListTag();
            for (NpcRoute.Waypoint waypoint : route.points()) {
                CompoundTag point = new CompoundTag();
                if (!waypoint.placeId().isBlank()) {
                    NpcPlace place = doc.place(waypoint.placeId()).orElse(null);
                    if (place == null) continue;
                    point.putDouble("x", place.x());
                    point.putDouble("y", place.y());
                    point.putDouble("z", place.z());
                    point.putString("dim", place.dimension());
                } else {
                    point.putDouble("x", waypoint.x());
                    point.putDouble("y", waypoint.y());
                    point.putDouble("z", waypoint.z());
                    point.putString("dim", fallbackDim);
                }
                points.add(point);
            }
            entry.put("points", points);
            routes.add(entry);
        }
        tag.put("routes", routes);
        PacketDistributor.sendToPlayer(player, new NpcPayloads.Client("overlay", tag));
    }


    public static void applyWand(ServerPlayer player, ItemStack stack, BlockPos pos) {
        if (!player.hasPermissions(2)) return;
        String documentId = NpcWandItem.documentId(stack);
        NpcCatalog catalog = NpcCatalog.get(player.server);
        NpcDocument doc = catalog.document(documentId).orElse(null);
        if (doc == null) {
            player.displayClientMessage(Component.translatable("npc.exworld.wand_pick"), true);
            return;
        }
        int mode = NpcWandItem.mode(stack);
        String dimension = player.serverLevel().dimension().location().toString();
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 1;
        double z = pos.getZ() + 0.5;
        NpcDocument next = switch (mode) {
            case 1 -> setHome(doc, dimension, x, y, z, player.getYRot());
            case 2 -> addWaypoint(doc, player, dimension, x, y, z);
            case 3 -> setPost(doc, dimension, x, y, z, player.getYRot());
            case 4 -> popWaypoint(doc);
            default -> doc;
        };
        if (mode == 0) {
            player.displayClientMessage(Component.translatable("npc.exworld.selected", pos.getX(), pos.getY(), pos.getZ()), true);
            return;
        }
        if (mode == 4 && next == doc) {
            player.displayClientMessage(Component.translatable("npc.exworld.nothing_undo"), true);
            return;
        }
        catalog.put(next, catalog.outgoing(next.id()));
        pushTool(player);
        player.displayClientMessage(Component.translatable("npc.exworld.wand_wrote", Component.translatable("npc.exworld.mode." + NpcWandItem.MODES[mode])), true);
    }

     private static NpcDocument popWaypoint(NpcDocument doc) {
         NpcRoute route = doc.route(doc.activeRouteId()).orElse(null);
         if (route == null || route.points().isEmpty()) return doc;
         List<NpcRoute> routes = new ArrayList<>();
         for (NpcRoute item : doc.routes()) routes.add(item.id().equals(route.id()) ? item.dropLast() : item);
         return doc.withRoutes(routes);
     }
 
     public static int despawn(MinecraftServer server, String id) {
         if (server == null || id == null || id.isBlank()) return 0;
         int count = 0;
         for (ServerLevel level : server.getAllLevels()) {
             List<UrbanNpc> found = new ArrayList<>();
             for (Entity entity : level.getAllEntities()) {
                 if (entity instanceof UrbanNpc npc && id.equals(npc.documentId())) found.add(npc);
             }
             for (UrbanNpc npc : found) {
                 npc.discard();
                 count++;
             }
         }
         return count;
     }
 
     public static List<RelationEdge> retarget(List<RelationEdge> edges, String toId) {
         List<RelationEdge> next = new ArrayList<>();
         if (edges == null || toId == null || toId.isBlank()) return next;
         for (RelationEdge edge : edges) next.add(new RelationEdge(toId, edge.toId(), edge.type(), edge.affinity(), edge.note()));
         return next;
     }


    public static UrbanNpc spawn(ServerPlayer player, String id) {
        NpcCatalog catalog = NpcCatalog.get(player.server);
        NpcDocument doc = catalog.document(id).orElse(null);
        if (doc == null) return null;
        if (!doc.anchored()) {
            doc = anchor(doc, player.serverLevel().dimension().location().toString(), player.getX(), player.getY(), player.getZ());
            catalog.put(doc, catalog.outgoing(id));
        }
        UrbanNpc npc = ExWorldContent.URBAN_NPC.get().create(player.serverLevel());
        if (npc == null) return null;
        npc.setDocumentId(id);
        npc.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0);
        player.serverLevel().addFreshEntity(npc);
        return npc;
    }

    public static NpcDocument blank(String id) {
        return new NpcDocument(id, id, "", "home", "", "talk", false,
                List.of(new NpcPlace("home", 0, 64, 0, "minecraft:overworld", 0, 1.5)),
                List.of(), List.of(new ActionSpec("wait", "wait", java.util.Map.of(), List.of())),
                List.of(), List.of(new DialogScript("talk", DialogScript.DialogMode.FIXED, List.of("……"), List.of(),
                net.exmo.exworld.npc.data.DialogBackground.DEFAULT, "", "……", 1)),
                List.of(), List.of(), net.exmo.exworld.npc.data.AiSettings.DEFAULT, net.exmo.exworld.npc.data.NpcLoadout.EMPTY);
    }

    private static NpcDocument setHome(NpcDocument doc, String dimension, double x, double y, double z, float yaw) {
        return anchor(doc, dimension, x, y, z).withHome("home");
    }

    private static NpcDocument setPost(NpcDocument doc, String dimension, double x, double y, double z, float yaw) {
        NpcDocument anchored = doc.anchored() ? doc : anchor(doc, dimension, x, y, z);
        List<NpcPlace> places = new ArrayList<>(anchored.places());
        places.removeIf(place -> place.id().equals("post"));
        places.add(new NpcPlace("post", x, y, z, dimension, yaw, 1.25));
        return anchored.withPlaces(places, true);
    }

    private static NpcDocument addWaypoint(NpcDocument doc, ServerPlayer player, String dimension, double x, double y, double z) {
        NpcDocument anchored = doc.anchored() ? doc : anchor(doc, dimension, player.getX(), player.getY(), player.getZ());
        String routeId = anchored.activeRouteId().isBlank() ? "path" : anchored.activeRouteId();
        List<NpcRoute> routes = new ArrayList<>(anchored.routes());
        NpcRoute existing = anchored.route(routeId).orElse(null);
        List<NpcRoute.Waypoint> points = new ArrayList<>(existing == null ? List.of() : existing.points());
        points.add(new NpcRoute.Waypoint("", x, y, z));
        NpcRoute route = new NpcRoute(routeId, existing == null ? net.exmo.exworld.npc.data.RouteMode.PATHFIND : existing.mode(),
                existing == null || existing.loop(), existing == null ? 1 : existing.speed(), points);
        routes.removeIf(item -> item.id().equals(routeId));
        routes.add(route);
        return new NpcDocument(anchored.id(), anchored.displayName(), anchored.texture(), anchored.homePlaceId(), routeId,
                anchored.defaultDialogId(), true, anchored.places(), routes, anchored.actions(), anchored.timeline(),
                anchored.dialogs(), anchored.marginals(), anchored.trades(), anchored.ai(), anchored.loadout());
    }

    public static NpcDocument anchor(NpcDocument doc, String dimension, double x, double y, double z) {
        if (doc.anchored()) return doc;
        NpcPlace home = doc.place(doc.homePlaceId()).orElse(null);
        double dx = x - (home == null ? 0 : home.x());
        double dy = y - (home == null ? 64 : home.y());
        double dz = z - (home == null ? 0 : home.z());
        List<NpcPlace> places = new ArrayList<>();
        boolean hasHome = false;
        for (NpcPlace place : doc.places()) {
            hasHome |= place.id().equals("home") || place.id().equals(doc.homePlaceId());
            places.add(new NpcPlace(place.id(), place.x() + dx, place.y() + dy, place.z() + dz, dimension, place.yaw(), place.radius()));
        }
        if (!hasHome) places.add(new NpcPlace("home", x, y, z, dimension, 0, 1.5));
        List<NpcRoute> routes = new ArrayList<>();
        for (NpcRoute route : doc.routes()) {
            List<NpcRoute.Waypoint> points = new ArrayList<>();
            for (NpcRoute.Waypoint point : route.points()) {
                points.add(point.placeId().isBlank()
                        ? new NpcRoute.Waypoint("", point.x() + dx, point.y() + dy, point.z() + dz) : point);
            }
            routes.add(new NpcRoute(route.id(), route.mode(), route.loop(), route.speed(), points));
        }
        String homeId = doc.homePlaceId().isBlank() ? "home" : doc.homePlaceId();
        return new NpcDocument(doc.id(), doc.displayName(), doc.texture(), homeId, doc.activeRouteId(), doc.defaultDialogId(),
                true, places, routes, doc.actions(), doc.timeline(), doc.dialogs(), doc.marginals(), doc.trades(), doc.ai(), doc.loadout());
    }
}
