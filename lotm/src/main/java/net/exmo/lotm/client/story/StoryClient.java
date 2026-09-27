package net.exmo.lotm.client.story;

import net.exmo.lotm.story.StoryClientHooks;

public final class StoryClient {
    private StoryClient() {}

    public static void install() {
        StoryClientHooks.openShop = CoinShopScreen::open;
        StoryClientHooks.openBook = StoryReadScreen::open;
        StoryClientHooks.openLetter = MessageTokenScreen::open;
    }
}
