package net.exmo.exworld.npc.dialog;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.exmo.exworld.Config;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** OpenAI-compatible chat call. A blank URL is a configured miss, not a crash. The key never leaves the server. */
public final class HttpDialogModel implements DialogModel {
    private static final HttpClient CLIENT = HttpClient.newBuilder().build();
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(1, runnable -> {
        Thread thread = new Thread(runnable, "exworld-npc-dialog");
        thread.setDaemon(true);
        return thread;
    });

    @Override
    public String complete(DialogRequest request) throws Exception {
        String url = Config.npcAiUrl;
        if (url == null || url.isBlank()) return null;
        int timeout = Math.max(500, Config.npcAiTimeoutMs);
        String body = body(request);
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url.trim()))
                .timeout(Duration.ofMillis(timeout))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (Config.npcAiKey != null && !Config.npcAiKey.isBlank()) {
            builder.header("Authorization", "Bearer " + Config.npcAiKey);
        }
        HttpResponse<String> response = CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) return null;
        return readContent(response.body());
    }

    public CompletableFuture<String> request(DialogRequest dialogRequest) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return complete(dialogRequest);
            } catch (Exception ex) {
                return null;
            }
        }, EXECUTOR);
    }

    private static String body(DialogRequest request) {
        JsonObject root = new JsonObject();
        root.addProperty("model", Config.npcAiModel == null || Config.npcAiModel.isBlank() ? "gpt-4o-mini" : Config.npcAiModel);
        var messages = new com.google.gson.JsonArray();
        JsonObject system = new JsonObject();
        system.addProperty("role", "system");
        system.addProperty("content", "你是都市 NPC " + request.npcName()
                + "。当前节点 " + request.nodeId()
                + "。关系 " + request.relations()
                + "。用一两句口语回答，不要提到自己是模型。");
        messages.add(system);
        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        String recent = String.join(" / ", request.recent());
        user.addProperty("content", request.playerName() + "：" + (recent.isBlank() ? "你好" : recent));
        messages.add(user);
        root.add("messages", messages);
        return root.toString();
    }

    private static String readContent(String json) {
        if (json == null || json.isBlank()) return null;
        var root = JsonParser.parseString(json).getAsJsonObject();
        var choices = root.getAsJsonArray("choices");
        if (choices == null || choices.isEmpty()) return null;
        var message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
        if (message == null || !message.has("content")) return null;
        return message.get("content").getAsString();
    }
}
