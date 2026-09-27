package net.exmo.exphone;

import com.mojang.blaze3d.platform.NativeImage;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.*;

/** Local originals and a bounded GPU thumbnail cache, scoped to the connected world/account. */
final class PhonePhotoClient {
    private static final ExecutorService IO = Executors.newSingleThreadExecutor(r -> { Thread t = new Thread(r, "phone-album-io"); t.setDaemon(true); return t; });
    private static final LinkedHashMap<String, ResourceLocation> textures = new LinkedHashMap<>(32, .75f, true);
    private static final Map<String, ByteArrayOutputStream> incoming = new HashMap<>();
    private static final Map<String, Integer> indices = new HashMap<>();
    private static final Set<String> loading = new HashSet<>();
    private static final Map<String, Long> requested = new HashMap<>();
    private static final List<String> album = new ArrayList<>();
    private static final Map<String, int[]> pixelSize = new HashMap<>();
    private record PhotoInfo(long modifiedTime, long bytes) {}
    private static final Map<String, PhotoInfo> photoInfo = new HashMap<>();
    private static long albumBytes;
    private static Path directory;
    private static long generation;
    private static long albumGeneration;
    private static Runnable afterUpload;
    private static String uploadId = "";

    static void open() {
        Minecraft mc = Minecraft.getInstance();
        String world = mc.hasSingleplayerServer() ? mc.getSingleplayerServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toAbsolutePath().toString()
                : mc.getCurrentServer() == null ? "unknown" : mc.getCurrentServer().ip;
        String key = UUID.nameUUIDFromBytes(world.getBytes(java.nio.charset.StandardCharsets.UTF_8)) + "/" + mc.getUser().getProfileId();
        Path path = mc.gameDirectory.toPath().resolve("phone-album").resolve(key);
        if (path.equals(directory)) return;
        clear(); directory = path; long expected = albumGeneration;
        IO.execute(() -> {
            Map<String, PhotoInfo> found = new HashMap<>();
            try {
                Files.createDirectories(path);
                try (var files = Files.list(path)) {
                    files.filter(p -> p.getFileName().toString().endsWith(".jpg") && !p.getFileName().toString().contains("-thumb") && !p.getFileName().toString().startsWith("remote-"))
                            .forEach(p -> {
                                try {
                                    BasicFileAttributes attributes = Files.readAttributes(p, BasicFileAttributes.class);
                                    found.put(p.getFileName().toString().replace(".jpg", ""),
                                            new PhotoInfo(attributes.lastModifiedTime().toMillis(), attributes.size()));
                                } catch (IOException ignored) { }
                            });
                }
            } catch (IOException ignored) { }
            List<String> ids = new ArrayList<>(found.keySet());
            ids.sort(Comparator.comparingLong((String id) -> found.get(id).modifiedTime()).reversed());
            mc.execute(() -> {
                if (expected != albumGeneration || !path.equals(directory)) return;
                album.clear(); album.addAll(ids);
                photoInfo.clear(); photoInfo.putAll(found);
                albumBytes = found.values().stream().mapToLong(PhotoInfo::bytes).sum();
                PhoneApricity.albumChanged();
            });
        });
    }
    static List<String> album() { return List.copyOf(album); }

    static long storageBytes() { return albumBytes; }

    static long modifiedTime(String id) {
        PhotoInfo info = photoInfo.get(id);
        return info == null ? 0 : info.modifiedTime();
    }
    static void clear() {
        generation++;
        albumGeneration++;
        textures.values().forEach(Minecraft.getInstance().getTextureManager()::release);
        textures.clear(); loading.clear(); incoming.clear(); indices.clear(); requested.clear(); album.clear(); photoInfo.clear(); pixelSize.clear(); albumBytes = 0;
        afterUpload = null; uploadId = ""; directory = null;
    }
    static void releaseTextures() {
        generation++;
        textures.values().forEach(Minecraft.getInstance().getTextureManager()::release);
        textures.clear(); loading.clear(); requested.clear();
    }
    static void save(NativeImage pixels) {
        open(); Path path = directory; long expected = albumGeneration;
        String id = UUID.randomUUID().toString();
        IO.execute(() -> {
            try (pixels) {
                BufferedImage image = new BufferedImage(pixels.getWidth(), pixels.getHeight(), BufferedImage.TYPE_INT_RGB);
                for (int y = 0; y < pixels.getHeight(); y++) for (int x = 0; x < pixels.getWidth(); x++) {
                    int c = pixels.getPixelRGBA(x, y);
                    image.setRGB(x, pixels.getHeight() - 1 - y, (c & 0xff) << 16 | (c & 0xff00) | (c >>> 16 & 0xff));
                }
                Files.createDirectories(path);
                ByteArrayOutputStream bytes = new ByteArrayOutputStream(); ImageIO.write(image, "jpg", bytes);
                if (bytes.size() > PhonePhotoPayload.MAX_BYTES) throw new IOException("图片过大");
                Files.write(path.resolve(id + ".jpg"), bytes.toByteArray());
                ImageIO.write(PhonePhotos.thumbnail(image), "jpg", path.resolve(id + "-thumb.jpg").toFile());
                PhotoInfo info = new PhotoInfo(System.currentTimeMillis(), bytes.size());
                Minecraft.getInstance().execute(() -> {
                    if (expected != albumGeneration || !path.equals(directory)) return;
                    photoInfo.put(id, info); albumBytes += info.bytes();
                    album.addFirst(id); PhoneApricity.notice("照片已保存"); PhoneApricity.albumChanged();
                });
            } catch (Exception error) { Minecraft.getInstance().execute(() -> PhoneApricity.notice("保存照片失败：" + error.getMessage())); }
        });
    }
    static ResourceLocation texture(String id, boolean full) {
        if (!PhonePhotos.validId(id) || directory == null) return null;
        String key = id + (full ? "" : "-thumb");
        if (textures.containsKey(key)) { if (!pixelSize.containsKey(key)) rememberSize(directory, key, generation); return textures.get(key); }
        if (loading.contains(key)) return null;
        loading.add(key); Path root = directory; long expected = generation;
        IO.execute(() -> {
            try {
                Path file = root.resolve(key + ".jpg");
                if (!Files.exists(file)) file = root.resolve("remote-" + key + ".jpg");
                if (!Files.exists(file)) {
                    Minecraft.getInstance().execute(() -> {
                        if (expected != generation) return;
                        loading.remove(key);
                        long now = System.currentTimeMillis();
                        if (now - requested.getOrDefault(key, 0L) > 2000 && Minecraft.getInstance().getConnection() != null) {
                            requested.put(key, now);
                            PacketDistributor.sendToServer(new PhonePhotoPayload(id, full ? -2 : -1, 0, new byte[0]));
                        }
                    });
                    return;
                }
                BufferedImage image = ImageIO.read(file.toFile());
                if (image == null) throw new IOException("Unsupported photo format");
                NativeImage nativeImage = new NativeImage(image.getWidth(), image.getHeight(), false);
                for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
                    int c = image.getRGB(x, y);
                    nativeImage.setPixelRGBA(x, y, 0xff000000 | (c & 0xff) << 16 | (c & 0xff00) | (c >>> 16 & 0xff));
                }
                int width = image.getWidth();
                int height = image.getHeight();
                Minecraft.getInstance().execute(() -> {
                    if (expected != generation) { nativeImage.close(); return; }
                    ResourceLocation location = ResourceLocation.fromNamespaceAndPath(ExPhone.MODID, "phone/photos/" + key);
                    Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(nativeImage));
                    textures.put(key, location); pixelSize.put(key, new int[]{width, height}); loading.remove(key);
                    while (textures.size() > 64) { String first = textures.keySet().iterator().next(); Minecraft.getInstance().getTextureManager().release(textures.remove(first)); }
                    PhoneApricity.imagesChanged();
                });

            } catch (Exception error) { Minecraft.getInstance().execute(() -> { if (expected == generation) loading.remove(key); }); }
        });
        return null;
    }
    static int[] pixelSize(String id, boolean full) {
        int[] size = pixelSize.get(id + (full ? "" : "-thumb"));
        return size == null ? null : size.clone();
    }

    private static void rememberSize(Path root, String key, long expected) {
        String token = key + ":size";
        if (loading.contains(token)) return;
        loading.add(token);
        IO.execute(() -> {
            int[] measured = null;
            try {
                Path file = root.resolve(key + ".jpg");
                if (!Files.exists(file)) file = root.resolve("remote-" + key + ".jpg");
                if (Files.exists(file)) {
                    try (var input = ImageIO.createImageInputStream(file.toFile())) {
                        var readers = ImageIO.getImageReaders(input);
                        if (readers.hasNext()) {
                            var reader = readers.next();
                            try { reader.setInput(input); measured = new int[]{reader.getWidth(0), reader.getHeight(0)}; }
                            finally { reader.dispose(); }
                        }
                    }
                }
            } catch (Exception ignored) {
                measured = null;
            }
            int[] size = measured;
            Minecraft.getInstance().execute(() -> {
                if (expected == generation && size != null) pixelSize.put(key, size);
                loading.remove(token);
                if (expected == generation && size != null) PhoneApricity.imagesChanged();
            });
        });
    }
    static void upload(String id, Runnable success) {
        if (directory == null || !album.contains(id)) return;
        if (afterUpload != null) { PhoneApricity.notice("正在上传图片"); return; }
        afterUpload = success; uploadId = id; Path file = directory.resolve(id + ".jpg");
        IO.execute(() -> {
            try {
                byte[] bytes = Files.readAllBytes(file);
                int total = (bytes.length + PhonePhotoPayload.CHUNK - 1) / PhonePhotoPayload.CHUNK;
                Minecraft.getInstance().execute(() -> {
                    if (Minecraft.getInstance().getConnection() == null) return;
                    for (int i = 0; i < total; i++) PacketDistributor.sendToServer(new PhonePhotoPayload(id, i, total,
                            Arrays.copyOfRange(bytes, i * PhonePhotoPayload.CHUNK, Math.min(bytes.length, (i + 1) * PhonePhotoPayload.CHUNK))));
                });
            } catch (IOException error) { Minecraft.getInstance().execute(() -> { afterUpload = null; PhoneApricity.notice("读取照片失败"); }); }
        });
    }
    static void uploaded(JsonObject state) {
        if (!uploadId.equals(state.get("id").getAsString())) return;
        Runnable callback = afterUpload; afterUpload = null; uploadId = "";
        if (state.get("ok").getAsBoolean() && callback != null) callback.run();
        else PhoneApricity.notice(state.get("message").getAsString());
    }
    static void receive(PhonePhotoPayload packet) {
        if (directory == null || !PhonePhotos.validId(packet.id()) || Math.abs(packet.total()) > 128 || packet.total() == 0) return;
        String key = packet.id() + (packet.total() < 0 ? "-thumb" : "");
        if (packet.index() == 0) { incoming.put(key, new ByteArrayOutputStream()); indices.put(key, 0); }
        ByteArrayOutputStream bytes = incoming.get(key);
        if (bytes == null || packet.index() != indices.get(key) || bytes.size() + packet.bytes().length > PhonePhotoPayload.MAX_BYTES) return;
        bytes.writeBytes(packet.bytes()); indices.put(key, packet.index() + 1);
        if (packet.index() + 1 != Math.abs(packet.total())) return;
        incoming.remove(key); indices.remove(key); Path file = directory.resolve("remote-" + key + ".jpg");
        byte[] content = bytes.toByteArray(); long expected = generation;
        IO.execute(() -> {
            try { PhonePhotos.decode(content); Files.createDirectories(file.getParent()); Files.write(file, content); }
            catch (IOException ignored) { return; }
            Minecraft.getInstance().execute(() -> { if (expected == generation) { requested.remove(key); PhoneApricity.imagesChanged(); } });
        });
    }
    static void delete(String id) {
        if (directory == null || !album.remove(id)) return;
        PhotoInfo info = photoInfo.remove(id);
        if (info != null) albumBytes = Math.max(0, albumBytes - info.bytes());
        Path root = directory;
        IO.execute(() -> { try { Files.deleteIfExists(root.resolve(id + ".jpg")); Files.deleteIfExists(root.resolve(id + "-thumb.jpg")); } catch (IOException ignored) { } });
        PhoneApricity.albumChanged();
    }
}
