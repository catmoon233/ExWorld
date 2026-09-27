package net.exmo.lotm.sequence;

import net.exmo.lotm.guimi.GuimiPathways;

public final class GuimiPathwaysHarness {
    private GuimiPathwaysHarness() {}

    public static void main(String[] args) {
        check(GuimiPathways.guimiKey("lotm:warrior").orElse("").equals("giant"), "warrior is giant");
        check(GuimiPathways.guimiKey("thief").orElse("").equals("error"), "thief is error");
        check(GuimiPathways.guimiKey("lotm:apprentice").orElse("").equals("door"), "apprentice is door");
        check(GuimiPathways.guimiKey("sailor").orElse("").equals("tyrant"), "sailor is tyrant");
        check(GuimiPathways.guimiKey("wizard").orElse("").equals("hermit"), "wizard is hermit");
        check(GuimiPathways.guimiKey("spectator").orElse("").equals("visionary"), "spectator is visionary");
        check(GuimiPathways.guimiKey("red_priest").orElse("").equals("war"), "red priest is war");
        check(GuimiPathways.canonicalPath("giant").equals("warrior"), "giant canonical warrior");
        check(GuimiPathways.resolveCanonical("error").orElse("").equals("thief"), "error aliases to thief");
        check(GuimiPathways.resolveCanonical("door").orElse("").equals("apprentice"), "door aliases to apprentice");
        check(GuimiPathways.resolveCanonical("tyrant").orElse("").equals("sailor"), "tyrant aliases to sailor");
        check(GuimiPathways.resolveCanonical("hermit").orElse("").equals("wizard"), "hermit aliases to wizard");
        check(GuimiPathways.resolveCanonical("visionary").orElse("").equals("spectator"), "visionary aliases to spectator");
        check(GuimiPathways.resolveCanonical("war").orElse("").equals("red_priest"), "war aliases to red priest");
        check(GuimiPathways.resolveCanonical("guimi_mod:fool").orElse("").equals("fool"), "namespaced fool");
        check(GuimiPathways.resolveCanonical("guimi_mod:witch").orElse("").equals("witch"), "namespaced witch");
        check(GuimiPathways.canonicalPath("fool").equals("fool"), "fool stays fool");
        check(GuimiPathways.guimiKey("painter").isEmpty(), "painter has no guimi pathway");
        check(GuimiPathways.guimiKey("fate_circle").isEmpty(), "fate circle has no guimi pathway");
        check(GuimiPathways.guimiKey("primordial_hunger").isEmpty(), "hunger has no guimi pathway");
        check(GuimiPathways.canonicalToGuimi().size() == 22, "22 pathways");
        System.out.println("GuimiPathwaysHarness ok");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
