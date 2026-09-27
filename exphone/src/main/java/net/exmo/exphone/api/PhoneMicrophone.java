package net.exmo.exphone.api;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** Microphone PCM captured by the phone's voice plugin. Other mods can tap it. */
public final class PhoneMicrophone {
    private static final List<Consumer<short[]>> taps = new CopyOnWriteArrayList<>();

    private PhoneMicrophone() {}

    public static void tap(Consumer<short[]> listener) {
        if (listener != null) taps.add(listener);
    }

    public static void hear(short[] samples) {
        if (samples == null) return;
        for (Consumer<short[]> tap : taps) {
            try {
                tap.accept(samples);
            } catch (RuntimeException ignored) {
            }
        }
    }
}
