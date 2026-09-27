package net.exmo.exkeys.client;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.exmo.exkeys.ExKeys;
import net.exmo.exkeys.KeyPolicy;
import net.exmo.exkeys.KeyPolicyFiles;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Path;

public final class KeyCaches {
    private KeyCaches() {}

    public static Path localPath() {
        return FMLPaths.CONFIGDIR.get().resolve("exkeys-local.json");
    }

    public static Path remotePath() {
        return FMLPaths.CONFIGDIR.get().resolve("exkeys-remote.json");
    }

    public static KeyPolicy loadLocal() {
        return readPolicy(localPath());
    }

    public static boolean saveLocal(KeyPolicy policy) {
        return KeyPolicyFiles.write(localPath(), (policy == null ? KeyPolicy.EMPTY : policy).toJson());
    }

    public static KeyPolicy loadRemote(String server) {
        try {
            String json = KeyPolicyFiles.read(remotePath());
            if (json.isBlank()) return KeyPolicy.EMPTY;
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            String stored = root.has("server") && root.get("server").isJsonPrimitive() ? root.get("server").getAsString() : "";
            if (server == null || !server.equals(stored)) return KeyPolicy.EMPTY;
            KeyPolicy.ParseResult parsed = KeyPolicy.parse(json, false);
            return parsed.ok() ? parsed.policy() : KeyPolicy.EMPTY;
        } catch (RuntimeException | IOException ex) {
            ExKeys.LOGGER.warn("Ignoring unreadable ExKeys remote cache", ex);
            return KeyPolicy.EMPTY;
        }
    }

    public static boolean saveRemote(String server, KeyPolicy policy) {
        JsonObject root = JsonParser.parseString((policy == null ? KeyPolicy.EMPTY : policy).toJson()).getAsJsonObject();
        root.addProperty("server", server == null ? "" : server);
        return KeyPolicyFiles.write(remotePath(), new GsonBuilder().setPrettyPrinting().create().toJson(root));
    }

    private static KeyPolicy readPolicy(Path path) {
        try {
            KeyPolicy.ParseResult parsed = KeyPolicy.parse(KeyPolicyFiles.read(path), false);
            if (!parsed.ok()) {
                ExKeys.LOGGER.warn("Ignoring unreadable ExKeys cache at {}: {}", path, parsed.error());
                return KeyPolicy.EMPTY;
            }
            return parsed.policy();
        } catch (IOException ex) {
            ExKeys.LOGGER.warn("Failed to read ExKeys cache at {}", path, ex);
            return KeyPolicy.EMPTY;
        }
    }
}
