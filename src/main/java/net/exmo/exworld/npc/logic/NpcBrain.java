package net.exmo.exworld.npc.logic;

import net.exmo.exworld.npc.action.ActionRegistry;
import net.exmo.exworld.npc.data.ActionSpec;
import net.exmo.exworld.npc.data.MarginalBinding;
import net.exmo.exworld.npc.data.TimelineNode;
import net.exmo.exworld.npc.entity.NpcMovement;
import net.exmo.exworld.npc.marginal.MarginalRegistry;

import java.util.ArrayList;
import java.util.List;

/** Composes schedule, sequence and marginal reactions without knowing the entity class. */
public final class NpcBrain {
    private NpcBrain() {}

    public static BrainDecision tick(BrainContext ctx) {
        try {
            if (ctx == null) return BrainDecision.idle("missing-context");
            if (ctx.dialogOpen()) return BrainDecision.pause(ctx);
            MarginalBinding active = binding(ctx, ctx.activeMarginalId());
            if (active != null) {
                boolean expired = ctx.tick() >= ctx.marginalEndsAt();
                boolean still = MarginalRegistry.triggered(active.behaviorId(), ctx, active);
                MarginalBinding higher = best(ctx, active.priority());
                if (!expired && still && higher == null) return marginal(ctx, active, ctx.suspendedNodeId(), ctx.marginalEndsAt());
                if (higher != null && (!still || expired || higher.priority() > active.priority())) {
                    return startMarginal(ctx, higher, ctx.suspendedNodeId());
                }
                return resume(ctx);
            }
            MarginalBinding next = best(ctx, Integer.MIN_VALUE);
            if (next != null) return startMarginal(ctx, next, ctx.currentNodeId());
            return schedule(ctx, TimelineSelector.select(ctx.nodes(), ctx.minute()).orElse(null), false);
        } catch (RuntimeException ex) {
            return BrainDecision.idle("fault");
        }
    }

    private static BrainDecision resume(BrainContext ctx) {
        TimelineNode restored = find(ctx.nodes(), ctx.suspendedNodeId());
        if (restored != null && TimelineSelector.contains(restored, ctx.minute())) {
            return schedule(ctx, restored, true);
        }
        return schedule(ctx, TimelineSelector.select(ctx.nodes(), ctx.minute()).orElse(null), true);
    }

    private static BrainDecision schedule(BrainContext ctx, TimelineNode node, boolean resumed) {
        if (node == null) {
            if (!ctx.homePlaceId().isBlank() && ctx.placeIds().contains(ctx.homePlaceId())) {
                return new BrainDecision(resumed ? BrainDecision.Kind.RESUME : BrainDecision.Kind.SCHEDULE,
                        "", "", "go_home", ctx.homePlaceId(), "", "", 0, "", 0, ctx.stuck() >= NpcMovement.STUCK_LIMIT, "");
            }
            return new BrainDecision(resumed ? BrainDecision.Kind.RESUME : BrainDecision.Kind.IDLE,
                    "", "", "look_around", "", "", "", 0, "", 0, false, "no-node");
        }
        ActionSpec spec = ctx.actions().get(node.actionId());
        if (spec == null) return BrainDecision.idle("missing-action");
        Resolved resolved = resolve(ctx, spec, ctx.sequenceIndex(), node.placeId());
        if (resolved == null) return BrainDecision.idle("missing-action");
        if (needsPlace(resolved.type()) && (resolved.placeId().isBlank() || !ctx.placeIds().contains(resolved.placeId()))) {
            return BrainDecision.idle("missing-place");
        }
        if (needsRoute(resolved.type()) && (resolved.routeId().isBlank() || !ctx.routeIds().contains(resolved.routeId()))) {
            return BrainDecision.idle("missing-place");
        }
        return new BrainDecision(resumed ? BrainDecision.Kind.RESUME : BrainDecision.Kind.SCHEDULE,
                node.id(), resolved.id(), resolved.type(), resolved.placeId(), resolved.routeId(), "", 0, "",
                resolved.index(), ctx.stuck() >= NpcMovement.STUCK_LIMIT || ctx.pathFailed(), "");
    }

    private static Resolved resolve(BrainContext ctx, ActionSpec spec, int index, String nodePlace) {
        if (!known(ctx, spec.type()) && !"sequence".equals(spec.type())) return null;
        if (!"sequence".equals(spec.type())) {
            return new Resolved(spec.id(), spec.type(), placeOf(spec, nodePlace), spec.param("route", ""), index);
        }
        List<ActionSpec> children = children(ctx, spec);
        int cursor = Math.max(0, index);
        while (cursor < children.size()) {
            ActionSpec child = children.get(cursor);
            if (child != null && known(ctx, child.type()) && !"sequence".equals(child.type())) {
                return new Resolved(child.id(), child.type(), placeOf(child, nodePlace), child.param("route", ""), cursor);
            }
            cursor++;
        }
        return null;
    }

    private static List<ActionSpec> children(BrainContext ctx, ActionSpec spec) {
        List<ActionSpec> children = new ArrayList<>();
        for (String childId : spec.children()) {
            ActionSpec child = ctx.actions().get(childId);
            if (child != null) children.add(child);
        }
        return children;
    }

    private static String placeOf(ActionSpec spec, String nodePlace) {
        String place = spec.param("place", "");
        return place.isBlank() ? nodePlace : place;
    }

    private static boolean known(BrainContext ctx, String type) {
        if (type == null || type.isBlank()) return false;
        if (ctx.knownActionTypes().isEmpty()) return ActionRegistry.known(type);
        return ctx.knownActionTypes().contains(type) || ActionRegistry.known(type);
    }

    private static boolean needsPlace(String type) {
        return "hold_post".equals(type) || "trade".equals(type) || "go_home".equals(type);
    }

    private static boolean needsRoute(String type) {
        return "follow_route".equals(type);
    }

    private static BrainDecision startMarginal(BrainContext ctx, MarginalBinding binding, String suspended) {
        long ends = ctx.tick() + binding.durationTicks();
        return marginal(ctx, binding, suspended == null ? "" : suspended, ends);
    }

    private static BrainDecision marginal(BrainContext ctx, MarginalBinding binding, String suspended, long ends) {
        String type = switch (binding.behaviorId()) {
            case "flee_combat" -> ctx.homePlaceId().isBlank() ? "flee" : "go_home";
            case "yield" -> "yield";
            case "watch", "greet_known", "call_help" -> "watch";
            case "return_leash" -> "go_home";
            case "hide_hurt" -> "hide";
            default -> "idle";
        };
        String place = "go_home".equals(type) ? ctx.homePlaceId() : "";
        return new BrainDecision(BrainDecision.Kind.MARGINAL, ctx.currentNodeId(), "", type, place, "",
                binding.behaviorId(), ends, suspended, ctx.sequenceIndex(), "yield".equals(type), "");
    }

    private static MarginalBinding best(BrainContext ctx, int above) {
        MarginalBinding best = null;
        for (MarginalBinding binding : ctx.marginals()) {
            if (binding == null || !binding.enabled() || "resume".equals(binding.behaviorId())) continue;
            if (binding.priority() <= above) continue;
            if (!MarginalRegistry.triggered(binding.behaviorId(), ctx, binding)) continue;
            if (best == null || binding.priority() > best.priority()) best = binding;
        }
        return best;
    }

    private static MarginalBinding binding(BrainContext ctx, String id) {
        if (id == null || id.isBlank()) return null;
        for (MarginalBinding binding : ctx.marginals()) {
            if (id.equals(binding.behaviorId())) return binding;
        }
        return null;
    }

    private static TimelineNode find(List<TimelineNode> nodes, String id) {
        if (id == null || id.isBlank() || nodes == null) return null;
        for (TimelineNode node : nodes) if (node != null && id.equals(node.id())) return node;
        return null;
    }

    private record Resolved(String id, String type, String placeId, String routeId, int index) {}
}
