package net.exmo.exkeys;

public final class KeyPolicyTestHarness {
    public static void main(String[] args) {
        if (PolicyMode.choose(false, false, false) != PolicyMode.Kind.LOCAL) fail("main menu uses local cache");
        if (PolicyMode.choose(true, true, false) != PolicyMode.Kind.LOCAL) fail("singleplayer uses local cache");
        if (PolicyMode.choose(true, true, true) != PolicyMode.Kind.REMOTE) fail("published LAN uses the server");
        if (PolicyMode.choose(true, false, false) != PolicyMode.Kind.REMOTE) fail("dedicated server uses the server");

        KeyPolicy policy = KeyPolicy.EMPTY.with("key.jump", true, false).with("key.inventory", false, true);
        if (!policy.hides("key.jump") || policy.blocks("key.jump")) fail("hide does not imply block");
        if (policy.hides("key.inventory") || !policy.blocks("key.inventory")) fail("block does not imply hide");
        if (policy.with("key.jump", false, false).hides("key.jump")) fail("clearing both flags removes the rule");
        String longId = "k".repeat(129);
        if (policy.with(longId, true, true) != policy) fail("invalid id is ignored");
        if (policy.with("key mod.with space", true, false).hides("key mod.with space") == false) fail("spaces in key ids are allowed");

        KeyPolicy locked = policy.withConfigOpOnly(true);
        if (!locked.configOpOnly() || !locked.hides("key.jump")) fail("visibility flag keeps the key rules");
        if (!locked.with("key.drop", true, false).configOpOnly()) fail("editing a bind keeps the visibility flag");
        if (locked.showConfigTo(false, false)) fail("op-only hides the button from regular players");
        if (!locked.showConfigTo(false, true) || !locked.showConfigTo(true, false)) fail("owner and operator still see the button");
        if (!policy.showConfigTo(false, false)) fail("open mode shows the button to everyone");
        KeyPolicy.ParseResult round = KeyPolicy.parse(locked.toJson(), true);
        if (!round.ok() || !round.policy().hides("key.jump") || !round.policy().configOpOnly()) {
            fail("json round trip lost a flag");
        }
        if (!KeyPolicy.parse("{\"configOpOnly\":true}", true).policy().configOpOnly()) fail("visibility flag without entries");
        if (KeyPolicy.parse("{\"entries\":{\"" + longId + "\":{\"hide\":true}}}", true).ok()) fail("strict parse rejects bad ids");
        if (!KeyPolicy.parse("{\"entries\":{\"bad id\":{\"hide\":true},\"key.drop\":{\"block\":true}}}", false).policy().blocks("key.drop")) {
            fail("lenient parse keeps valid ids");
        }
        if (KeyPolicy.parse("{", true).ok()) fail("broken json is rejected");
        if (!KeyPolicy.parse("{}", true).ok() || !KeyPolicy.parse("{}", true).policy().ids().isEmpty()) fail("empty object is an empty policy");

        boolean[] keep = KeyListVisibility.keep(
                new boolean[] {true, false, false, true, false},
                new boolean[] {false, true, true, false, false});
        if (keep[0] || !keep[3] || !keep[4] || keep[1] || keep[2]) fail("empty categories are removed and visible binds stay");

        if (!PolicySearch.matches("jump", "key.jump", "Jump", "Movement", "Space")) fail("search matches name and id");
        if (PolicySearch.matches("boat", "key.jump", "Jump", "Movement", "Space")) fail("search rejects unrelated text");
        if (!PolicySearch.matches("  ", "key.jump", "Jump", "Movement", "Space")) fail("blank search matches everything");

        System.out.println("ExKeys policy tests passed");
    }

    private static void fail(String message) {
        throw new AssertionError(message);
    }
}
