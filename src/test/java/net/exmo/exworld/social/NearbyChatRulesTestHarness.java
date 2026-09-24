package net.exmo.exworld.social;

/** Audience rules for adventure proximity chat and hidden spectator speech. */
public final class NearbyChatRulesTestHarness {
    public static void main(String[] args) {
        var rules = NearbyChatRules.class;
        require(rules != null, "rules missing");
        var adventure = NearbyChatRules.Audience.ADVENTURE;
        var spectator = NearbyChatRules.Audience.SPECTATOR;
        var other = NearbyChatRules.Audience.OTHER;
        require(NearbyChatRules.audience(true, true) == spectator, "spectator wins over adventure");
        require(NearbyChatRules.audience(false, true) == adventure, "adventure audience");
        require(NearbyChatRules.rewrites(adventure) && NearbyChatRules.rewrites(spectator) && !NearbyChatRules.rewrites(other),
                "only adventure and spectator chat are rewritten");
        require(NearbyChatRules.hears(adventure, adventure, true, 30, 4, 0, 64, 32), "nearby adventure hears adventure");
        require(!NearbyChatRules.hears(adventure, adventure, true, 80, 0, 0, 64, 32), "far adventure does not hear");
        require(!NearbyChatRules.hears(other, adventure, true, 10, 40, 0, 64, 32), "vertical range blocks adventure chat");
        require(!NearbyChatRules.hears(other, adventure, false, 1, 0, 1, 64, 32), "other dimension does not hear adventure");
        require(NearbyChatRules.hears(adventure, adventure, true, 0, 0, 0, 64, 32), "speaker hears themselves");
        require(!NearbyChatRules.hears(adventure, spectator, true, 1, 0, 1, 64, 32), "adventure cannot hear spectators");
        require(NearbyChatRules.hears(spectator, spectator, false, 999, 999, 999, 64, 32), "spectators hear spectators anywhere");
        require(NearbyChatRules.hears(other, spectator, false, 999, 0, 0, 64, 32), "non-adventure hears spectator chat");
        var topics = ExworldHelpCatalog.topics();
        require(topics.stream().anyMatch(topic -> topic.id().equals("chat")), "chat help missing");
        require(topics.stream().anyMatch(topic -> topic.id().equals("fight")), "fight help missing");
        require(topics.stream().flatMap(topic -> topic.lines().stream()).allMatch(line -> line.usage().startsWith("/")),
                "help usage must be a command");
        System.out.println("NEARBY_CHAT_RULES_TEST_OK");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
