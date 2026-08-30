package net.exmo.exworld.battle.compat;

import net.minecraft.world.damagesource.DamageSource;

import java.util.function.Supplier;

/** Marks a nested vanilla damage call as battle-authorized without bypassing the vanilla damage pipeline. */
public final class BattleDamageContext {
    private static final ThreadLocal<State> ACTIVE = new ThreadLocal<>();
    private BattleDamageContext() {}

    public static <T> T call(DamageSource source, Supplier<T> action) {
        State previous = ACTIVE.get(); State state = new State(source);
        ACTIVE.set(state);
        try { return action.get(); }
        finally { if (previous == null) ACTIVE.remove(); else ACTIVE.set(previous); }
    }
    public static boolean active() { return ACTIVE.get() != null; }
    public static DamageSource source() { State state = ACTIVE.get(); return state == null ? null : state.source; }
    public static void markFatal() { State state = ACTIVE.get(); if (state != null) state.fatal = true; }
    public static boolean fatal() { State state = ACTIVE.get(); return state != null && state.fatal; }
    private static final class State { private final DamageSource source; private boolean fatal; private State(DamageSource source){this.source=source;} }
}
