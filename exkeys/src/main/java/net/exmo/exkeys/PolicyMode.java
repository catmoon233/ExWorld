package net.exmo.exkeys;

/** Singleplayer and the main menu use the local cache. A dedicated server or an open LAN world uses the server file. */
public final class PolicyMode {
    private PolicyMode() {}

    public enum Kind {
        LOCAL,
        REMOTE
    }

    public static Kind choose(boolean inWorld, boolean singleplayer, boolean published) {
        if (!inWorld || (singleplayer && !published)) return Kind.LOCAL;
        return Kind.REMOTE;
    }
}
