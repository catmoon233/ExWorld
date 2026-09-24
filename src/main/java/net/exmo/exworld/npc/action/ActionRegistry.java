package net.exmo.exworld.npc.action;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ActionRegistry {
    private static final Map<String, NpcAction> ACTIONS = new LinkedHashMap<>();

    static {
        BuiltinActions.register();
    }

    private ActionRegistry() {}

    public static void register(NpcAction action) {
        if (action != null && action.type() != null && !action.type().isBlank()) ACTIONS.put(action.type(), action);
    }

    public static boolean known(String type) {
        return type != null && ACTIONS.containsKey(type);
    }

    public static NpcAction get(String type) {
        return ACTIONS.get(type);
    }

    public static Collection<String> ids() {
        return Collections.unmodifiableSet(ACTIONS.keySet());
    }
}
