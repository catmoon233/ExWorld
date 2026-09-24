package net.exmo.exworld.npc.action;

/** Built-in action types. New types can be registered without changing a document schema. */
public final class BuiltinActions {
    private BuiltinActions() {}

    public static void register() {
        simple("idle", false, false);
        simple("look_around", false, false);
        simple("interact_nearby", false, false);
        simple("hold_post", true, false);
        simple("follow_route", false, true);
        simple("go_home", true, false);
        simple("wait", false, false);
        simple("trade", true, false);
        simple("speak", false, false);
        simple("sequence", false, false);
        simple("flee", false, false);
        simple("yield", false, false);
        simple("watch", false, false);
        simple("hide", false, false);
    }

    private static void simple(String type, boolean place, boolean route) {
        ActionRegistry.register(new NpcAction() {
            @Override public String type() { return type; }
            @Override public boolean needsPlace() { return place; }
            @Override public boolean needsRoute() { return route; }
        });
    }
}
