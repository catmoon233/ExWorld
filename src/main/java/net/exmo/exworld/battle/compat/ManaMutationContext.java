package net.exmo.exworld.battle.compat;

import java.util.function.Supplier;

/** Allows intentional battle mana gains while ordinary tick regeneration remains blocked. */
public final class ManaMutationContext {
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);
    private ManaMutationContext() {}
    public static boolean allowed() { return DEPTH.get() > 0; }
    public static void run(Runnable action) { call(() -> { action.run(); return null; }); }
    public static <T> T call(Supplier<T> action) {
        DEPTH.set(DEPTH.get() + 1);
        try { return action.get(); }
        finally { int next = DEPTH.get() - 1; if (next == 0) DEPTH.remove(); else DEPTH.set(next); }
    }
}
