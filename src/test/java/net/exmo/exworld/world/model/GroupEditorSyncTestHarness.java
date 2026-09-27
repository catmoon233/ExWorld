package net.exmo.exworld.world.model;

/** The editor must show the same void the M map shows, and a client save must not regenerate that void away. */
public final class GroupEditorSyncTestHarness {
    public static void main(String[] args) {
        require(GroupEditorSync.blankMap(true), "decryption M map is blank");
        require(!GroupEditorSync.blankMap(false), "ordinary maps are not forced blank");
        require(!GroupEditorSync.paintAutomaticBiomes(true, false), "blank map must not paint generated biomes");
        require(!GroupEditorSync.paintAutomaticBiomes(false, true), "archipelago void must not paint generated biomes");
        require(GroupEditorSync.paintAutomaticBiomes(false, false), "ordinary maps still paint the biome atlas");
        require(!GroupEditorSync.listed(true, false, false, false), "unconfigured automatic groups stay off the blank editor");
        require(GroupEditorSync.listed(true, true, false, false), "groups shown on the M map stay in the editor");
        require(GroupEditorSync.listed(true, false, true, false), "the group being edited remains reachable");
        require(GroupEditorSync.listed(false, false, false, false), "ordinary editors still list automatic groups");
        require(GroupEditorSync.persistDraft(true, false), "a client editing the blank map must persist the draft");
        require(!GroupEditorSync.persistDraft(false, false), "automatic mode still regenerates when the map is not blank");
        require(GroupEditorSync.persistDraft(false, true), "manual drafts still persist");
        require(!GroupEditorSync.generateAutomaticGroups(true, false), "decryption mode must not generate automatic groups");
        require(GroupEditorSync.generateAutomaticGroups(false, false), "ordinary new worlds still generate automatic groups");
        require(!GroupEditorSync.generateAutomaticGroups(false, true), "manual worlds must not regenerate automatic groups");
        require(GroupEditorSync.inDragBox(2, 3, 4, 1, 0, 3) && !GroupEditorSync.inDragBox(5, 3, 4, 1, 0, 3),
                "drag selection is the inclusive rectangle between the press and the pointer");
        require(!GroupEditorSync.assignedGroup(true, false), "an unconfigured decryption cell is not a group");
        require(GroupEditorSync.assignedGroup(true, true), "a configured decryption group still bounds the world");
        require(GroupEditorSync.assignedGroup(false, false), "ordinary automatic zones remain groups");
        System.out.println("GROUP_EDITOR_SYNC_TEST_OK");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
