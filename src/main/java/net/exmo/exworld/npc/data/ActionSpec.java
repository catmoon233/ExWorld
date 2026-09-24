package net.exmo.exworld.npc.data;

import java.util.List;
import java.util.Map;

/** A document-local action. type is a registry id; children compose other document action ids. */
public record ActionSpec(String id, String type, Map<String, String> params, List<String> children) {
    public ActionSpec {
        id = id == null ? "" : id.trim();
        type = type == null ? "" : type.trim();
        params = params == null ? Map.of() : Map.copyOf(params);
        children = children == null ? List.of() : List.copyOf(children);
    }

    public String param(String key, String fallback) {
        String value = params.get(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
