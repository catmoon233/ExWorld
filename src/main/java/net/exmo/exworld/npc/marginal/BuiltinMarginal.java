package net.exmo.exworld.npc.marginal;

import net.exmo.exworld.npc.data.MarginalBinding;
import net.exmo.exworld.npc.logic.BrainContext;

/** Built-in interruptions. flee_combat is the worked example; the others are usable as-is. */
public final class BuiltinMarginal {
    private BuiltinMarginal() {}

    public static void register() {
        add("flee_combat", (ctx, binding) -> ctx.combatNearby());
        add("yield", (ctx, binding) -> ctx.blocked() || ctx.waypointBlocked());
        add("watch", (ctx, binding) -> ctx.interactionNearby() && !ctx.combatNearby());
        add("greet_known", (ctx, binding) -> ctx.knownVisible() && ctx.knownAffinity() >= binding.paramInt("minAffinity", 0));
        add("return_leash", (ctx, binding) -> ctx.leashedAway());
        add("hide_hurt", (ctx, binding) -> ctx.hurt());
        add("call_help", (ctx, binding) -> ctx.hurt());
        add("resume", (ctx, binding) -> false);
    }

    private static void add(String id, Trigger trigger) {
        MarginalRegistry.register(new MarginalBehavior() {
            @Override public String id() { return id; }
            @Override public boolean triggered(BrainContext context, MarginalBinding binding) {
                return trigger.test(context, binding);
            }
        });
    }

    @FunctionalInterface
    private interface Trigger {
        boolean test(BrainContext context, MarginalBinding binding);
    }
}
