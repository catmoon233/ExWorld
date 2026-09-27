package net.exmo.exphone.api;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Other mods can lock the phone camera or hear when the viewfinder holds the camera. */
public final class PhoneCameraControl {
    private static final List<BooleanSupplier> locks = new CopyOnWriteArrayList<>();
    private static final List<Consumer<Boolean>> held = new CopyOnWriteArrayList<>();

    private PhoneCameraControl() {}

    public static void lock(BooleanSupplier lock) {
        if (lock != null) locks.add(lock);
    }

    public static void onHeld(Consumer<Boolean> listener) {
        if (listener != null) held.add(listener);
    }

    public static boolean locked() {
        for (BooleanSupplier lock : locks) {
            try {
                if (lock.getAsBoolean()) return true;
            } catch (RuntimeException ignored) {
            }
        }
        return false;
    }

    public static void held(boolean value) {
        for (Consumer<Boolean> listener : held) {
            try {
                listener.accept(value);
            } catch (RuntimeException ignored) {
            }
        }
    }
}
