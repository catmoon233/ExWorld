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

        KeyPolicy opOnly = policy.with("key.drop", false, false, true, true);
        if (opOnly.hides("key.drop") || opOnly.blocks("key.drop")) fail("op-only flags are not absolute");
        if (!opOnly.opTrigger("key.drop") || !opOnly.opDisplay("key.drop")) fail("op-only flags are stored");
        if (opOnly.hiddenFrom("key.drop", true) || !opOnly.hiddenFrom("key.drop", false)) fail("op display hides only non-operators");
        if (opOnly.blockedFor("key.drop", true) || !opOnly.blockedFor("key.drop", false)) fail("op trigger blocks only non-operators");
        if (!policy.hiddenFrom("key.jump", true) || !policy.blockedFor("key.inventory", true)) fail("absolute flags still apply to operators");
        if (opOnly.with("key.drop", true, true, true, true).rule("key.drop").opDisplay()) fail("hide overrides op display");
        if (opOnly.with("key.drop", true, true, true, true).rule("key.drop").opTrigger()) fail("block overrides op trigger");
        if (opOnly.with("key.drop", false, false, false, false).ids().contains("key.drop")) fail("clearing every flag removes the rule");
        KeyPolicy.ParseResult round = KeyPolicy.parse(opOnly.toJson(), true);
        if (!round.ok() || !round.policy().opTrigger("key.drop") || !round.policy().opDisplay("key.drop") || round.policy().toJson().contains("configOpOnly")) {
            fail("json round trip lost an op-only flag");
        }
        KeyPolicy legacy = KeyPolicy.parse("{\"configOpOnly\":true,\"entries\":{\"key.jump\":{\"hide\":true}}}", true).policy();
        if (!legacy.hides("key.jump") || legacy.toJson().contains("configOpOnly")) fail("legacy visibility flag is ignored");
        if (!KeyPolicy.parse("{\"entries\":{\"key.use\":{\"opTrigger\":true}}}", true).policy().blockedFor("key.use", false)) {
            fail("op trigger without other flags still blocks non-operators");
        }
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
