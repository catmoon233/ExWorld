package net.exmo.exworld.npc.entity;

import net.exmo.exworld.npc.data.ActionSpec;
import net.exmo.exworld.npc.data.AiSettings;
import net.exmo.exworld.npc.data.MarginalBinding;
import net.exmo.exworld.npc.data.NpcCatalog;
import net.exmo.exworld.npc.data.NpcDocument;
import net.exmo.exworld.npc.data.NpcPlace;
import net.exmo.exworld.npc.data.NpcRoute;
import net.exmo.exworld.npc.data.RelationEdge;
import net.exmo.exworld.npc.logic.BrainContext;
import net.exmo.exworld.npc.logic.BrainDecision;
import net.exmo.exworld.npc.logic.DayClock;
import net.exmo.exworld.npc.logic.NpcBrain;
import net.exmo.exworld.npc.logic.RelationGraph;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Turns sensor facts into a brain decision and applies it. A missing target becomes idle, not an exception. */
public final class NpcRuntime {
    private NpcRuntime() {}

    public static void tick(UrbanNpc npc) {
        if (!(npc.level() instanceof ServerLevel level) || level.getServer() == null) return;
        NpcCatalog catalog = NpcCatalog.get(level.getServer());
        NpcDocument doc = catalog.document(npc.documentId()).orElse(null);
        if (doc == null) {
            npc.safeIdle("missing-document");
            return;
        }
        sync(npc, doc, catalog.revision());
        if (npc.dialogOpen()) {
            npc.getNavigation().stop();
            return;
        }
        BrainDecision decision = NpcBrain.tick(sense(npc, doc, catalog));
        apply(npc, doc, catalog, decision);
    }

    private static void sync(UrbanNpc npc, NpcDocument doc, int revision) {
        if (npc.seenRevision() != revision) {
            npc.setSeenRevision(revision);
            npc.setTextureId(doc.texture());
            npc.setCustomName(net.minecraft.network.chat.Component.literal(doc.displayName()));
            npc.setCustomNameVisible(true);
            npc.applyLoadout(doc);
        }
        npc.restock(doc, npc.level().getGameTime());
    }

    private static BrainContext sense(UrbanNpc npc, NpcDocument doc, NpcCatalog catalog) {
        AiSettings ai = doc.ai();
        double radius = marginalRadius(doc, 8);
        Set<String> places = new HashSet<>();
        for (NpcPlace place : doc.places()) places.add(place.id());
        Set<String> routes = new HashSet<>();
        for (NpcRoute route : doc.routes()) routes.add(route.id());
        Map<String, ActionSpec> actions = new HashMap<>();
        for (ActionSpec action : doc.actions()) actions.put(action.id(), action);
        boolean combat = safe(() -> combatNearby(npc, radius));
        boolean blocked = safe(() -> npc.horizontalCollision || !npc.level().getEntitiesOfClass(LivingEntity.class,
                npc.getBoundingBox().inflate(0.3, 0.1, 0.3), entity -> entity != npc && entity.isAlive()).isEmpty());
        boolean hurt = npc.hurtTime > 0;
        boolean leashed = safe(() -> leashedAway(npc, doc, radius));
        RelationEdge known = safeRelation(npc, doc, catalog, radius);
        boolean pathFailed = npc.getNavigation().isDone() && npc.stuck() > 0;
        boolean interaction = safe(() -> npc.level().getNearestPlayer(npc, ai.lookDistance()) != null);
        return new BrainContext(DayClock.minuteOfDay(npc.level().getDayTime()), npc.level().getGameTime(), npc.dialogOpen(),
                combat, blocked, hurt, leashed, known != null, known == null ? 0 : known.affinity(), pathFailed, blocked, interaction,
                npc.stuck(), npc.nodeId(), npc.suspendedNodeId(), npc.marginalId(), npc.marginalEndsAt(), npc.sequenceIndex(),
                doc.timeline(), doc.marginals(), actions, places, routes, doc.homePlaceId(), Set.of());
    }

    private static void apply(UrbanNpc npc, NpcDocument doc, NpcCatalog catalog, BrainDecision decision) {
        if (decision.kind() == BrainDecision.Kind.PAUSE) {
            npc.getNavigation().stop();
            npc.setTrading(false);
            return;
        }
        if (decision.kind() == BrainDecision.Kind.MARGINAL) {
            npc.setMarginal(decision.marginalId(), decision.marginalEndsAt(), decision.suspendedNodeId());
        } else {
            npc.clearMarginal();
            if (!decision.nodeId().isBlank()) npc.setNodeId(decision.nodeId());
        }
        npc.setSequenceIndex(decision.sequenceIndex());
        if (decision.kind() == BrainDecision.Kind.IDLE || "idle".equals(decision.actionType()) && !decision.idleReason().isBlank()) {
            npc.safeIdle(decision.idleReason().isBlank() ? "idle" : decision.idleReason());
            return;
        }
        npc.clearIdle();
        npc.setTrading(false);
        npc.setTradePlace("");
        if (npc.hurtTime > 0) callHelp(npc, doc, catalog);
        switch (decision.actionType()) {
            case "go_home", "hold_post" -> moveToPlace(npc, doc, decision.placeId().isBlank() ? doc.homePlaceId() : decision.placeId(), decision.sideStep(), 1.0);
            case "trade" -> {
                String place = decision.placeId().isBlank() ? doc.homePlaceId() : decision.placeId();
                npc.setTradePlace(place);
                npc.setTrading(moveToPlace(npc, doc, place, decision.sideStep(), 1.0));
            }
            case "follow_route" -> follow(npc, doc, decision.routeId().isBlank() ? doc.activeRouteId() : decision.routeId(), decision.sideStep());
            case "flee" -> flee(npc, decision.sideStep());
            case "yield" -> side(npc);
            case "hide" -> retreat(npc);
            case "watch" -> look(npc, doc);
            case "look_around" -> npc.getLookControl().setLookAt(npc.getX() + Math.sin(npc.tickCount * 0.08), npc.getEyeY(), npc.getZ() + Math.cos(npc.tickCount * 0.08));
            case "interact_nearby" -> interact(npc, doc);
            case "speak" -> npc.setSpeech(line(doc, decision.actionId()), 80);
            case "wait" -> npc.getNavigation().stop();
            default -> npc.safeIdle("missing-action");
        }
    }

    private static boolean moveToPlace(UrbanNpc npc, NpcDocument doc, String placeId, boolean sideStep, double speed) {
        NpcPlace place = doc.place(placeId).orElse(null);
        if (place == null) {
            npc.safeIdle("missing-place");
            return false;
        }
        if (!place.dimension().equals(npc.level().dimension().location().toString())) {
            npc.safeIdle("missing-place");
            return false;
        }
        boolean there = NpcMovement.arrived(npc.getX(), npc.getY(), npc.getZ(), place.x(), place.y(), place.z(), place.radius());
        if (there) {
            npc.getNavigation().stop();
            npc.setYRot(place.yaw());
            npc.setStuck(0);
            return true;
        }
        if (sideStep) {
            side(npc);
            npc.setStuck(0);
            return false;
        }
        npc.getNavigation().moveTo(place.x(), place.y(), place.z(), speed);
        npc.setStuck(NpcMovement.stuckAfter(npc.stuck(), npc.getNavigation().isDone()));
        return false;
    }

    private static void follow(UrbanNpc npc, NpcDocument doc, String routeId, boolean sideStep) {
        NpcRoute route = doc.route(routeId).orElse(null);
        if (route == null || route.points().isEmpty()) {
            npc.safeIdle("missing-place");
            return;
        }
        int index = Math.min(npc.routeIndex(), route.points().size() - 1);
        NpcRoute.Waypoint point = route.points().get(index);
        double[] at = resolve(doc, point);
        if (at == null) {
            npc.safeIdle("missing-place");
            return;
        }
        boolean there = NpcMovement.arrived(npc.getX(), npc.getY(), npc.getZ(), at[0], at[1], at[2], 1.4);
        if (there) {
            npc.setRouteIndex(NpcMovement.nextIndex(index, route.points().size(), route.loop(), true));
            npc.setStuck(0);
            return;
        }
        if (sideStep || (route.mode() == net.exmo.exworld.npc.data.RouteMode.MANUAL && npc.horizontalCollision && npc.stuck() > 0)) {
            if (npc.stuck() >= 2 && npc.stuck() < NpcMovement.STUCK_LIMIT) {
                npc.getNavigation().stop();
                npc.setStuck(NpcMovement.stuckAfter(npc.stuck(), true));
                return;
            }
            side(npc);
            return;
        }
        if (route.mode() == net.exmo.exworld.npc.data.RouteMode.MANUAL) {
            npc.getNavigation().stop();
            npc.getMoveControl().setWantedPosition(at[0], at[1], at[2], route.speed());
            npc.setStuck(NpcMovement.stuckAfter(npc.stuck(), npc.horizontalCollision));
        } else {
            npc.getNavigation().moveTo(at[0], at[1], at[2], route.speed());
            npc.setStuck(NpcMovement.stuckAfter(npc.stuck(), npc.getNavigation().isDone() && !there));
        }
    }

    private static double[] resolve(NpcDocument doc, NpcRoute.Waypoint point) {
        if (!point.placeId().isBlank()) {
            NpcPlace place = doc.place(point.placeId()).orElse(null);
            if (place == null) return null;
            return new double[] { place.x(), place.y(), place.z() };
        }
        return new double[] { point.x(), point.y(), point.z() };
    }

    private static void side(UrbanNpc npc) {
        double[] step = NpcMovement.sideStep(npc.getX(), npc.getZ(), npc.getYRot(), npc.stuck());
        npc.getNavigation().stop();
        npc.getMoveControl().setWantedPosition(step[0], npc.getY(), step[1], 1.0);
        npc.setStuck(NpcMovement.stuckAfter(npc.stuck(), true));
    }

    private static void flee(UrbanNpc npc, boolean sideStep) {
        LivingEntity threat = npc.level().getNearestEntity(LivingEntity.class, net.minecraft.world.entity.ai.targeting.TargetingConditions.forNonCombat().range(12), npc, npc.getX(), npc.getY(), npc.getZ(), npc.getBoundingBox().inflate(12));
        if (threat == null || sideStep) {
            side(npc);
            return;
        }
        Vec3 away = npc.position().subtract(threat.position());
        if (away.lengthSqr() < 0.01) away = new Vec3(1, 0, 0);
        away = away.normalize().scale(6);
        npc.getNavigation().moveTo(npc.getX() + away.x, npc.getY(), npc.getZ() + away.z, 1.3);
    }

    private static void retreat(UrbanNpc npc) {
        LivingEntity threat = npc.getLastHurtByMob();
        if (threat == null) {
            npc.getNavigation().stop();
            return;
        }
        Vec3 away = npc.position().subtract(threat.position()).normalize().scale(3);
        npc.getNavigation().moveTo(npc.getX() + away.x, npc.getY(), npc.getZ() + away.z, 1.1);
    }

    private static void look(UrbanNpc npc, NpcDocument doc) {
        npc.getNavigation().stop();
        Player player = npc.level().getNearestPlayer(npc, doc.ai().lookDistance());
        if (player != null) npc.getLookControl().setLookAt(player);
    }

    private static void interact(UrbanNpc npc, NpcDocument doc) {
        look(npc, doc);
        if (!doc.ai().useDoors() && !doc.ai().faceWorkBlocks()) return;
        BlockPos front = npc.blockPosition().relative(npc.getDirection());
        BlockState state = npc.level().getBlockState(front);
        try {
            if (doc.ai().useDoors() && state.getBlock() instanceof DoorBlock door && state.hasProperty(BlockStateProperties.OPEN)) {
                door.setOpen(npc, npc.level(), state, front, !state.getValue(BlockStateProperties.OPEN));
            } else if (doc.ai().faceWorkBlocks() && (state.getBlock() instanceof BarrelBlock || state.getBlock() instanceof LecternBlock
                    || state.is(net.minecraft.world.level.block.Blocks.CRAFTING_TABLE) || state.is(net.minecraft.world.level.block.Blocks.FURNACE))) {
                npc.getLookControl().setLookAt(Vec3.atCenterOf(front));
            }
        } catch (RuntimeException ignored) {
        }
    }

    private static void callHelp(UrbanNpc npc, NpcDocument doc, NpcCatalog catalog) {
        MarginalBinding binding = doc.marginal("call_help").orElse(null);
        if (binding == null || !binding.enabled()) return;
        LivingEntity source = npc.getLastHurtByMob();
        if (source == null) return;
        RelationGraph graph = new RelationGraph(catalog.relations());
        AABB box = npc.getBoundingBox().inflate(binding.radius());
        for (UrbanNpc other : npc.level().getEntitiesOfClass(UrbanNpc.class, box, entity -> entity != npc && entity.isAlive())) {
            try {
                RelationEdge edge = graph.find(npc.documentId(), other.documentId()).orElse(null);
                if (edge == null || !edge.ally()) continue;
                NpcDocument otherDoc = catalog.document(other.documentId()).orElse(null);
                if (otherDoc == null || otherDoc.marginal("call_help").filter(MarginalBinding::enabled).isEmpty()) continue;
                other.getLookControl().setLookAt(source);
            } catch (RuntimeException ignored) {
            }
        }
    }

    private static String line(NpcDocument doc, String actionId) {
        ActionSpec spec = doc.action(actionId).orElse(null);
        if (spec == null) return "";
        String direct = spec.param("line", "");
        if (!direct.isBlank()) return direct;
        return doc.dialog(spec.param("dialog", "")).map(dialog -> dialog.primaryLine()).orElse("");
    }

    private static boolean combatNearby(UrbanNpc npc, double radius) {
        AABB box = npc.getBoundingBox().inflate(radius);
        for (LivingEntity living : npc.level().getEntitiesOfClass(LivingEntity.class, box, entity -> entity != npc && entity.isAlive())) {
            if (living.hurtTime > 0 || living.getLastHurtByMob() != null) return true;
        }
        return false;
    }

    private static boolean leashedAway(UrbanNpc npc, NpcDocument doc, double radius) {
        if (!npc.isLeashed()) return false;
        NpcPlace home = doc.place(doc.homePlaceId()).orElse(null);
        if (home == null) return npc.distanceToSqr(npc.getX(), npc.getY(), npc.getZ()) > 0 && false;
        return npc.distanceToSqr(home.x(), home.y(), home.z()) > radius * radius;
    }

    private static RelationEdge safeRelation(UrbanNpc npc, NpcDocument doc, NpcCatalog catalog, double radius) {
        try {
            RelationGraph graph = new RelationGraph(catalog.relations());
            UrbanNpc other = npc.level().getNearestEntity(UrbanNpc.class, net.minecraft.world.entity.ai.targeting.TargetingConditions.forNonCombat().range(radius),
                    npc, npc.getX(), npc.getY(), npc.getZ(), npc.getBoundingBox().inflate(radius));
            if (other == null) return null;
            return graph.find(doc.id(), other.documentId()).orElse(null);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static double marginalRadius(NpcDocument doc, double fallback) {
        double radius = fallback;
        for (MarginalBinding binding : doc.marginals()) if (binding.enabled()) radius = Math.max(radius, binding.radius());
        return radius;
    }

    private static boolean safe(Sensor sensor) {
        try {
            return sensor.get();
        } catch (RuntimeException ex) {
            return false;
        }
    }

    @FunctionalInterface
    private interface Sensor { boolean get(); }
}
