package net.exmo.exworld.npc.logic;

/** One safe decision. The world tick applies it and never has to interpret a thrown error. */
public record BrainDecision(
        Kind kind,
        String nodeId,
        String actionId,
        String actionType,
        String placeId,
        String routeId,
        String marginalId,
        long marginalEndsAt,
        String suspendedNodeId,
        int sequenceIndex,
        boolean sideStep,
        String idleReason) {
    public enum Kind { SCHEDULE, MARGINAL, IDLE, PAUSE, RESUME }

    public BrainDecision {
        nodeId = nodeId == null ? "" : nodeId;
        actionId = actionId == null ? "" : actionId;
        actionType = actionType == null ? "" : actionType;
        placeId = placeId == null ? "" : placeId;
        routeId = routeId == null ? "" : routeId;
        marginalId = marginalId == null ? "" : marginalId;
        suspendedNodeId = suspendedNodeId == null ? "" : suspendedNodeId;
        idleReason = idleReason == null ? "" : idleReason;
    }

    public static BrainDecision idle(String reason) {
        return new BrainDecision(Kind.IDLE, "", "", "idle", "", "", "", 0, "", 0, false, reason);
    }

    public static BrainDecision pause(BrainContext ctx) {
        return new BrainDecision(Kind.PAUSE, ctx.currentNodeId(), "", "", "", "", ctx.activeMarginalId(),
                ctx.marginalEndsAt(), ctx.suspendedNodeId(), ctx.sequenceIndex(), false, "");
    }
}
