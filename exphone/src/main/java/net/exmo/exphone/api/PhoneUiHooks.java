package net.exmo.exphone.api;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Runs before the phone screen opens. Other mods can close their own overlays here. */
public final class PhoneUiHooks {
    private static final List<Runnable> beforeOpen = new CopyOnWriteArrayList<>();

    private PhoneUiHooks() {}

    public static void beforeOpen(Runnable hook) {
        if (hook != null) beforeOpen.add(hook);
    }

    public static void opening() {
        for (Runnable hook : beforeOpen) {
            try {
                hook.run();
            } catch (RuntimeException ignored) {
            }
        }
    }
}
