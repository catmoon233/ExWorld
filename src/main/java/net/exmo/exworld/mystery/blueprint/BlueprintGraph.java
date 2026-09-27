package net.exmo.exworld.mystery.blueprint;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.exmo.exworld.mystery.Era;
import net.exmo.exworld.mystery.GamePhase;
import net.exmo.exworld.mystery.MysterySave;
import net.exmo.exworld.mystery.MysterySounds;
import net.exmo.exworld.mystery.PlayerState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The one authoring and runtime shape used by datapacks and DM drafts. */
public record BlueprintGraph(ResourceLocation id, int version, List<Node> nodes, List<Edge> edges) {
    public record Node(String id, String type, JsonObject params, float x, float y) {
        public String text(String key) { return params.has(key) ? params.get(key).getAsString() : ""; }
        public int number(String key, int fallback) { return params.has(key) ? params.get(key).getAsInt() : fallback; }
    }
    public record Edge(String from, String port, String to) {}

    public BlueprintGraph {
        nodes = List.copyOf(nodes);
        edges = List.copyOf(edges);
    }

    public static BlueprintGraph parse(String json) {
        BlueprintGraph graph = parseDraft(json);
        graph.validate();
        return graph;
    }

    /** Editor loading accepts incomplete graphs; only publication and runtime require validation. */
    public static BlueprintGraph parseDraft(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        ResourceLocation id = ResourceLocation.parse(root.get("id").getAsString());
        int version = root.has("version") ? root.get("version").getAsInt() : 1;
        List<Node> nodes = new ArrayList<>();
        for (JsonElement element : root.getAsJsonArray("nodes")) {
            JsonObject node = element.getAsJsonObject();
            JsonObject params = node.has("params") ? node.getAsJsonObject("params") : new JsonObject();
            nodes.add(new Node(node.get("id").getAsString(), node.get("type").getAsString(), params,
                    node.has("x") ? node.get("x").getAsFloat() : 0,
                    node.has("y") ? node.get("y").getAsFloat() : 0));
        }
        List<Edge> edges = new ArrayList<>();
        if (root.has("edges")) for (JsonElement element : root.getAsJsonArray("edges")) {
            JsonObject edge = element.getAsJsonObject();
            String from = edge.get("from").getAsString();
            String port = edge.has("port") ? edge.get("port").getAsString() : "next";
            edges.add(new Edge(from, port, edge.get("to").getAsString()));
        }
        BlueprintGraph graph = new BlueprintGraph(id, version, nodes, edges);
        return graph;
    }

    public String json() {
        JsonObject root = new JsonObject();
        root.addProperty("id", id.toString()); root.addProperty("version", version);
        JsonArray ns = new JsonArray();
        for (Node node : nodes) {
            JsonObject value = new JsonObject();
            value.addProperty("id", node.id()); value.addProperty("type", node.type());
            value.add("params", node.params().deepCopy());
            value.addProperty("x", node.x()); value.addProperty("y", node.y());
            ns.add(value);
        }
        root.add("nodes", ns);
        JsonArray es = new JsonArray();
        for (Edge edge : edges) {
            JsonObject value = new JsonObject();
            value.addProperty("from", edge.from()); value.addProperty("port", edge.port()); value.addProperty("to", edge.to());
            es.add(value);
        }
        root.add("edges", es);
        return root.toString();
    }

    public Node node(String id) {
        for (Node node : nodes) if (node.id().equals(id)) return node;
        return null;
    }

    public List<Node> next(String id, String port) {
        List<Node> result = new ArrayList<>();
        for (Edge edge : edges) if (edge.from().equals(id) && edge.port().equals(port)) result.add(node(edge.to()));
        return result;
    }

    public void validate() {
        if (version != 1 || nodes.isEmpty() || nodes.size() > 256 || edges.size() > 512)
            throw new IllegalArgumentException("unsupported or oversized blueprint");
        Map<String, Node> byId = new HashMap<>();
        for (Node node : nodes) {
            if (!node.id().matches("[a-zA-Z0-9_.-]{1,64}") || byId.putIfAbsent(node.id(), node) != null)
                throw new IllegalArgumentException("invalid or duplicate node " + node.id());
            if (!Set.of("trigger", "condition", "action", "cue", "wait").contains(node.type()))
                throw new IllegalArgumentException("unknown node type " + node.type());
            validateParams(node);
        }
        boolean trigger = nodes.stream().anyMatch(node -> node.type().equals("trigger"));
        if (!trigger) throw new IllegalArgumentException("missing trigger");
        for (Edge edge : edges) {
            Node from = byId.get(edge.from()), to = byId.get(edge.to());
            if (from == null || to == null) throw new IllegalArgumentException("missing edge endpoint");
            if (to.type().equals("trigger")) throw new IllegalArgumentException("trigger cannot have input");
            if (!validPort(from, edge.port())) throw new IllegalArgumentException("invalid port " + edge.port());
            if (edges.stream().filter(other -> other.from().equals(edge.from()) && other.port().equals(edge.port())).count() > 1)
                throw new IllegalArgumentException("more than one edge on " + edge.from() + ":" + edge.port());
        }
        Set<String> seen = new HashSet<>(), visiting = new HashSet<>();
        for (Node node : nodes) if (node.type().equals("trigger")) visit(node.id(), visiting, seen);
        if (seen.size() != nodes.size()) throw new IllegalArgumentException("unreachable nodes");
    }

    /** Publication checks world resources and the identities currently defined in the lobby. */
    public void validateResources(MinecraftServer server) {
        for (Node node : nodes) {
            String value = node.text("value");
            if (node.type().equals("cue") && !MysterySounds.known(node.text("id")))
                throw new IllegalArgumentException("unknown cue " + node.text("id"));
            if (node.type().equals("condition") && node.text("kind").equals("character")
                    && MysterySave.get(server).participants().stream().noneMatch(p -> p.characterId.equals(value)))
                throw new IllegalArgumentException("undefined identity " + value);
            if (!node.type().equals("action")) continue;
            if (node.text("kind").equals("give_item")) {
                ResourceLocation item = ResourceLocation.tryParse(value);
                if (item == null || !BuiltInRegistries.ITEM.containsKey(item))
                    throw new IllegalArgumentException("missing item " + value);
            }
            if (node.text("kind").equals("run_function")) {
                ResourceLocation function = ResourceLocation.tryParse(value);
                if (function == null || server.getFunctions().get(function).isEmpty())
                    throw new IllegalArgumentException("missing function " + value);
            }
        }
    }

    private void visit(String id, Set<String> visiting, Set<String> seen) {
        if (seen.contains(id)) return;
        if (!visiting.add(id)) throw new IllegalArgumentException("blueprint cycle at " + id);
        for (Edge edge : edges) if (edge.from().equals(id)) visit(edge.to(), visiting, seen);
        visiting.remove(id); seen.add(id);
    }

    private static boolean validPort(Node from, String port) {
        return from.type().equals("condition") ? port.equals("true") || port.equals("false") : port.equals("next");
    }

    private static void validateParams(Node node) {
        String action = node.text("kind");
        switch (node.type()) {
            case "trigger" -> {
                if (!Set.of("on_phase_enter", "on_player_death", "on_clue_found", "on_seal_use", "on_npc_choice").contains(action))
                    throw new IllegalArgumentException("unknown trigger " + action);
            }
            case "condition" -> {
                if (!Set.of("era", "state", "phase", "character", "has_clue", "seal_min", "cycle_min").contains(action))
                    throw new IllegalArgumentException("unknown condition " + action);
                if (action.equals("era")) enumValue(Era.class, node.text("value"));
                if (action.equals("phase")) enumValue(GamePhase.class, node.text("value"));
                if (action.equals("state")) enumValue(PlayerState.class, node.text("value"));
                if (action.equals("character") && !node.text("value").matches("[a-z0-9_.-]{1,64}"))
                    throw new IllegalArgumentException("invalid identity id");
            }
            case "action" -> {
                if (!Set.of("add_clue", "add_suspicion", "give_item", "run_function", "reset_timeline", "finish").contains(action))
                    throw new IllegalArgumentException("unknown action " + action);
                if (action.equals("run_function") && !node.text("value").startsWith("exworld:mystery/"))
                    throw new IllegalArgumentException("functions must be under exworld:mystery/");
                if (action.equals("add_clue") && !node.text("value").matches("[a-z0-9_.:-]{1,96}"))
                    throw new IllegalArgumentException("invalid clue id");
                if (action.equals("give_item") && ResourceLocation.tryParse(node.text("value")) == null)
                    throw new IllegalArgumentException("invalid item id");
            }
            case "cue" -> {
                if (!node.text("id").matches("[a-z0-9_.-]{1,64}")) throw new IllegalArgumentException("invalid cue id");
                if (node.number("duration", 80) < 1 || node.number("duration", 80) > 1200)
                    throw new IllegalArgumentException("invalid cue duration");
            }
            case "wait" -> {
                if (node.number("ticks", 0) < 1 || node.number("ticks", 0) > 20 * 60 * 30)
                    throw new IllegalArgumentException("invalid wait");
            }
            default -> throw new IllegalArgumentException("unknown node");
        }
    }

    private static <E extends Enum<E>> void enumValue(Class<E> type, String value) {
        try { Enum.valueOf(type, value.toUpperCase(java.util.Locale.ROOT)); }
        catch (RuntimeException error) { throw new IllegalArgumentException("invalid " + type.getSimpleName() + " " + value); }
    }
}
