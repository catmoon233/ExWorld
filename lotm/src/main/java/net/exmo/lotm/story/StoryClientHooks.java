package net.exmo.lotm.story;

import java.util.function.Consumer;

/** Client screens install these. Dedicated servers keep the no-op defaults. */
public final class StoryClientHooks {
    public static Runnable openShop = () -> {};
    public static Consumer<String> openBook = key -> {};
    public static Consumer<String> openLetter = text -> {};

    private StoryClientHooks() {}
}
