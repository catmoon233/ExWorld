package net.exmo.exkeys;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Which keybind ids are hidden, blocked, or restricted to operators. */
public final class KeyPolicy {
    public static final int MAX_ENTRIES = 512;
    public static final int MAX_JSON_CHARS = 200_000;
    public static final KeyPolicy EMPTY = new KeyPolicy(Map.of());

    private final Map<String, Rule> rules;

    public KeyPolicy(Map<String, Rule> rules) {
        this.rules = Map.copyOf(rules);
    }

    public Set<String> ids() {
        return rules.keySet();
    }

    public Rule rule(String id) {
        Rule rule = rules.get(id);
        return rule == null ? Rule.NONE : rule;
    }

    public boolean hides(String id) {
        return rule(id).hide();
    }

    public boolean blocks(String id) {
        return rule(id).block();
    }

    public boolean opTrigger(String id) {
        return rule(id).opTrigger();
    }

    public boolean opDisplay(String id) {
        return rule(id).opDisplay();
    }

    /** Hide applies to everyone. OP-only display hides the bind from non-operators. */
    public boolean hiddenFrom(String id, boolean operator) {
        Rule rule = rule(id);
        return rule.hide() || (rule.opDisplay() && !operator);
    }

    /** Block applies to everyone. OP-only trigger blocks the bind for non-operators. */
    public boolean blockedFor(String id, boolean operator) {
        Rule rule = rule(id);
        return rule.block() || (rule.opTrigger() && !operator);
    }

    public KeyPolicy with(String id, boolean hide, boolean block) {
        return with(id, hide, block, false, false);
    }

    public KeyPolicy with(String id, boolean hide, boolean block, boolean opTrigger, boolean opDisplay) {
        if (!validId(id)) return this;
        Rule rule = new Rule(hide, block, opTrigger, opDisplay);
        Map<String, Rule> next = new LinkedHashMap<>(rules);
        if (!rule.active()) next.remove(id);
        else next.put(id, rule);
        return new KeyPolicy(next);
    }

    public String toJson() {
        JsonObject entries = new JsonObject();
        for (Map.Entry<String, Rule> entry : rules.entrySet()) {
            JsonObject rule = new JsonObject();
            rule.addProperty("hide", entry.getValue().hide());
            rule.addProperty("block", entry.getValue().block());
            rule.addProperty("opTrigger", entry.getValue().opTrigger());
            rule.addProperty("opDisplay", entry.getValue().opDisplay());
            entries.add(entry.getKey(), rule);
        }
        JsonObject root = new JsonObject();
        root.add("entries", entries);
        return new GsonBuilder().setPrettyPrinting().create().toJson(root);
    }

    public static ParseResult parse(String json, boolean strict) {
        if (json == null || json.isBlank()) return new ParseResult(EMPTY, true, "");
        if (json.length() > MAX_JSON_CHARS) return new ParseResult(EMPTY, false, "too large");
        try {
            JsonElement parsed = JsonParser.parseString(json);
            if (!parsed.isJsonObject()) return new ParseResult(EMPTY, false, "root must be an object");
            JsonObject root = parsed.getAsJsonObject();
            if (!root.has("entries")) return new ParseResult(EMPTY, true, "");
            JsonObject entries = root.getAsJsonObject("entries");
            if (entries.size() > MAX_ENTRIES) return new ParseResult(EMPTY, false, "too many entries");
            Map<String, Rule> rules = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : entries.entrySet()) {
                if (!validId(entry.getKey())) {
                    if (strict) return new ParseResult(EMPTY, false, "invalid id");
                    continue;
                }
                if (!entry.getValue().isJsonObject()) {
                    if (strict) return new ParseResult(EMPTY, false, "rule must be an object");
                    continue;
                }
                JsonObject rule = entry.getValue().getAsJsonObject();
                Rule parsedRule = new Rule(
                        booleanField(rule, "hide"),
                        booleanField(rule, "block"),
                        booleanField(rule, "opTrigger"),
                        booleanField(rule, "opDisplay"));
                if (parsedRule.active()) rules.put(entry.getKey(), parsedRule);
            }
            return new ParseResult(new KeyPolicy(rules), true, "");
        } catch (RuntimeException ex) {
            return new ParseResult(EMPTY, false, "invalid json");
        }
    }

    public static boolean validId(String id) {
        if (id == null || id.isEmpty() || id.length() > 128) return false;
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            if (c < 0x20 || c == 0x7F) return false;
        }
        return true;
    }

    private static boolean booleanField(JsonObject object, String name) {
        return object.has(name) && object.get(name).isJsonPrimitive() && object.get(name).getAsJsonPrimitive().isBoolean()
                && object.get(name).getAsBoolean();
    }

    public record Rule(boolean hide, boolean block, boolean opTrigger, boolean opDisplay) {
        public static final Rule NONE = new Rule(false, false, false, false);

        public Rule {
            if (hide) opDisplay = false;
            if (block) opTrigger = false;
        }

        public boolean active() {
            return hide || block || opTrigger || opDisplay;
        }
    }

    public record ParseResult(KeyPolicy policy, boolean ok, String error) {}
}
