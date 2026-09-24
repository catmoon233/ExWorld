package net.exmo.exworld.npc.marginal;

import net.exmo.exworld.npc.data.MarginalBinding;
import net.exmo.exworld.npc.logic.BrainContext;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MarginalRegistry {
    private static final Map<String, MarginalBehavior> BEHAVIORS = new LinkedHashMap<>();

    static {
        BuiltinMarginal.register();
    }

    private MarginalRegistry() {}

    public static void register(MarginalBehavior behavior) {
        if (behavior != null && behavior.id() != null && !behavior.id().isBlank()) BEHAVIORS.put(behavior.id(), behavior);
    }

    public static boolean known(String id) {
        return id != null && BEHAVIORS.containsKey(id);
    }

    public static boolean triggered(String id, BrainContext context, MarginalBinding binding) {
        MarginalBehavior behavior = BEHAVIORS.get(id);
        if (behavior == null || binding == null || !binding.enabled() || context == null) return false;
        try {
            return behavior.triggered(context, binding);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    public static Collection<String> ids() {
        return Collections.unmodifiableSet(BEHAVIORS.keySet());
    }
}
