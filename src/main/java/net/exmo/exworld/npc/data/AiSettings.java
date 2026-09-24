package net.exmo.exworld.npc.data;

public record AiSettings(
        double lookDistance,
        double strollRadius,
        boolean groundNavigation,
        boolean noticePlayers,
        boolean noticeNpcs,
        boolean useDoors,
        boolean faceWorkBlocks) {
    public static final AiSettings DEFAULT = new AiSettings(8, 6, true, true, true, true, true);

    public AiSettings {
        lookDistance = lookDistance <= 0 ? 8 : lookDistance;
        strollRadius = Math.max(0, strollRadius);
    }
}
