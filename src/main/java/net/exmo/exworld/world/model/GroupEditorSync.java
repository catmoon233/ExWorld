package net.exmo.exworld.world.model;

/**
 * Keeps the region-group editor on the same picture the M map actually shows.
 * A blank decryption map and an archipelago void must not be painted with the automatic biome layout,
 * and a client save has to persist that picture instead of regenerating it on the server.
 */
public final class GroupEditorSync {
    private GroupEditorSync() {}

    /** Decryption mode's M map is intentionally empty until an admin turns a group on. */
    public static boolean blankMap(boolean decryptionMode) { return decryptionMode; }

    /** Decryption mode never builds biome-zone groups; the atlas stays a blank manual partition. */
    public static boolean generateAutomaticGroups(boolean decryptionMode, boolean manualGroups) {
        return !decryptionMode && !manualGroups;
    }

    /**
     * Automatic mode discards the draft and rebuilds zones. That must not happen while the live map is blank,
     * or the editor keeps showing generated groups the M map does not have.
     */
    public static boolean persistDraft(boolean decryptionMode, boolean manualGroups) {
        return decryptionMode || manualGroups;
    }

    /** Decryption cells stay ordinary world until an admin turns their group on. */
    public static boolean assignedGroup(boolean decryptionMode, boolean configured) {
        return !decryptionMode || configured;
    }

    /** Inclusive map-cell rectangle used by the editor's drag selection. */
    public static boolean inDragBox(int mapX, int mapZ, int startX, int startZ, int endX, int endZ) {
        int minX = Math.min(startX, endX);
        int maxX = Math.max(startX, endX);
        int minZ = Math.min(startZ, endZ);
        int maxZ = Math.max(startZ, endZ);
        return mapX >= minX && mapX <= maxX && mapZ >= minZ && mapZ <= maxZ;
    }

    /** Unconfigured automatic groups stay in the save payload, but they are not map content. */
    public static boolean listed(boolean blankMap, boolean configured, boolean active, boolean selected) {
        return !blankMap || configured || active || selected;
    }

    /** The biome atlas is the generated layout. Blank and island maps leave those cells as void. */
    public static boolean paintAutomaticBiomes(boolean blankMap, boolean archipelago) {
        return !blankMap && !archipelago;
    }
}
