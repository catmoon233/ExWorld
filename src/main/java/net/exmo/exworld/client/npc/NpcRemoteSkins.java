package net.exmo.exworld.client.npc;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import net.exmo.exworld.Exworld;
import net.exmo.exworld.npc.skin.SkinUrls;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.ByteArrayInputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Client skin cache. https and http are both accepted; private hosts and non-PNG bodies are not. */
public final class NpcRemoteSkins {
    public static final ResourceLocation STEVE = ResourceLocation.withDefaultNamespace("textures/entity/steve.png");
    private static final int MAX_BYTES = 512 * 1024;
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NEVER).build();
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "exworld-npc-skin");
        thread.setDaemon(true);
        return thread;
    });
    private static final Map<String, ResourceLocation> READY = new ConcurrentHashMap<>();
    private static final Set<String> PENDING = ConcurrentHashMap.newKeySet();
    private static final Set<String> FAILED = ConcurrentHashMap.newKeySet();

    private NpcRemoteSkins() {}

    public static ResourceLocation resolve(String raw, String kind) {
        if (raw == null || raw.isBlank()) return STEVE;
        String value = raw.trim();
        String mode = kind == null ? "" : kind.trim().toLowerCase();
        if (mode.isBlank()) mode = SkinUrls.http(value) ? "url" : value.startsWith("player:") ? "player" : "resource";
        if ("player".equals(mode) || value.startsWith("player:")) {
            String name = value.startsWith("player:") ? value.substring(7) : value;
            return fetch("player:" + name, () -> playerUrl(name));
        }
        if ("url".equals(mode) || SkinUrls.http(value)) {
            String url = value.startsWith("url:") ? value.substring(4) : value;
            return fetch(url, () -> url);
        }
        try {
            return ResourceLocation.parse(value.startsWith("resource:") ? value.substring(9) : value);
        } catch (RuntimeException ex) {
            return STEVE;
        }
    }

    private static ResourceLocation fetch(String key, UrlSource source) {
        ResourceLocation ready = READY.get(key);
        if (ready != null) return ready;
        if (!FAILED.contains(key) && PENDING.add(key)) {
            CompletableFuture.supplyAsync(() -> download(key, source), EXECUTOR).whenComplete((image, error) -> Minecraft.getInstance().execute(() -> {
                PENDING.remove(key);
                if (image == null) {
                    FAILED.add(key);
                    return;
                }
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Exworld.MODID, "npc_skin/" + Integer.toUnsignedString(key.hashCode()));
                Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(image));
                READY.put(key, id);
            }));
        }
        return STEVE;
    }

    private static NativeImage download(String key, UrlSource source) {
        try {
            Path cache = Minecraft.getInstance().gameDirectory.toPath().resolve("exworld-npc-skins").resolve(Integer.toUnsignedString(key.hashCode()) + ".png");
            if (Files.isRegularFile(cache) && Files.size(cache) > 0 && Files.size(cache) <= MAX_BYTES) {
                NativeImage cached = NativeImage.read(Files.newInputStream(cache));
                if (acceptable(cached)) return cached;
                cached.close();
            }
            String url = source.get();
            byte[] body = readPublicPng(url, 0);
            if (body == null) return null;
            Files.createDirectories(cache.getParent());
            Files.write(cache, body);
            NativeImage image = NativeImage.read(new ByteArrayInputStream(body));
            if (!acceptable(image)) {
                image.close();
                return null;
            }
            return image;
        } catch (Exception ex) {
            return null;
        }
    }

    private static String playerUrl(String name) throws Exception {
        if (name == null || name.isBlank() || !name.matches("[A-Za-z0-9_]{1,16}")) return null;
        byte[] profile = readPublicPng("https://api.mojang.com/users/profiles/minecraft/" + name, 0, false);
        if (profile == null) return null;
        JsonObject named = JsonParser.parseString(new String(profile)).getAsJsonObject();
        if (!named.has("id")) return null;
        byte[] session = readPublicPng("https://sessionserver.mojang.com/session/minecraft/profile/" + named.get("id").getAsString(), 0, false);
        if (session == null) return null;
        JsonObject root = JsonParser.parseString(new String(session)).getAsJsonObject();
        if (!root.has("properties") || root.getAsJsonArray("properties").isEmpty()) return null;
        String encoded = root.getAsJsonArray("properties").get(0).getAsJsonObject().get("value").getAsString();
        JsonObject textures = JsonParser.parseString(new String(Base64.getDecoder().decode(encoded))).getAsJsonObject();
        return textures.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString();
    }

    private static byte[] readPublicPng(String url, int redirects) {
        return readPublicPng(url, redirects, true);
    }

    private static byte[] readPublicPng(String url, int redirects, boolean requirePng) {
        if (url == null || redirects > 3 || SkinUrls.reject(url) != null) return null;
        try {
            URI uri = URI.create(url);
            InetAddress[] addresses = InetAddress.getAllByName(uri.getHost());
            for (InetAddress address : addresses) if (SkinUrls.privateAddress(address)) return null;
            HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(8)).header("User-Agent", "ExWorld-NPC").GET().build();
            HttpResponse<byte[]> response = HTTP.send(request, HttpResponse.BodyHandlers.ofByteArray());
            int status = response.statusCode();
            if (status >= 300 && status < 400) {
                String next = response.headers().firstValue("location").orElse("");
                if (next.startsWith("/")) next = uri.getScheme() + "://" + uri.getHost() + next;
                return readPublicPng(next, redirects + 1, requirePng);
            }
            if (status < 200 || status >= 300) return null;
            byte[] body = response.body();
            if (body == null || body.length == 0 || body.length > MAX_BYTES) return null;
            if (requirePng && (body.length < 8 || body[0] != (byte) 0x89 || body[1] != 0x50)) return null;
            return body;
        } catch (Exception ex) {
            return null;
        }
    }

    private static boolean acceptable(NativeImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        return width >= 64 && height >= 32 && width <= 256 && height <= 256 && width % 64 == 0 && (height % 32 == 0);
    }

    @FunctionalInterface
    private interface UrlSource {
        String get() throws Exception;
    }
}