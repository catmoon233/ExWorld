package net.exmo.exphone;

import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.network.PacketDistributor;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import javax.imageio.ImageIO;

/** Bounded uploads and authorized downloads. No client/rendering classes on the server. */
final class PhonePhotos {
    static final long QUOTA = Math.max(2, Long.getLong("exphone.phone.photoQuotaMiB", 512L)) * 1024 * 1024;
    private static final ExecutorService IO = Executors.newSingleThreadExecutor(r -> { Thread t = new Thread(r, "phone-photo-io"); t.setDaemon(true); return t; });
    private static final Map<UUID, Upload> uploads = new HashMap<>();
    private static final Set<UUID> busy = new HashSet<>();
    private static final Map<UUID, Long> downloads = new HashMap<>();
    private record Upload(String id, ByteArrayOutputStream bytes, int total, int next, long since) {}

    static void receive(ServerPlayer player, PhonePhotoPayload packet) {
        if (!validId(packet.id())) return;
        if (packet.index() < 0) { download(player, packet); return; }
        if (!PhoneWifi.online(player) || PhoneStacks.heldPhone(player).isEmpty()) { reply(player, packet.id(), false, "网络不可用"); return; }
        if (busy.contains(player.getUUID())) return;
        PhoneData data = PhoneData.get(player);
        if (data.photoOwners.containsKey(packet.id())) {
            reply(player, packet.id(), player.getUUID().toString().equals(data.photoOwners.get(packet.id())), "图片已上传"); return;
        }
        long now = System.currentTimeMillis();
        uploads.values().removeIf(upload -> now - upload.since > 30_000);
        Upload old = uploads.get(player.getUUID());
        if (packet.total() < 1 || packet.total() > PhonePhotoPayload.MAX_BYTES / PhonePhotoPayload.CHUNK || packet.bytes().length == 0) return;
        if (packet.index() == 0) old = new Upload(packet.id(), new ByteArrayOutputStream(), packet.total(), 0, now);
        if (old == null || !old.id.equals(packet.id()) || old.total != packet.total() || old.next != packet.index()) return;
        if (old.bytes.size() + packet.bytes().length > PhonePhotoPayload.MAX_BYTES) { uploads.remove(player.getUUID()); return; }
        old.bytes.writeBytes(packet.bytes());
        Upload next = new Upload(old.id, old.bytes, old.total, old.next + 1, old.since);
        uploads.put(player.getUUID(), next);
        if (next.next != next.total) return;
        uploads.remove(player.getUUID()); busy.add(player.getUUID());
        Path root = root(player);
        UUID owner = player.getUUID();
        byte[] bytes = next.bytes.toByteArray();
        IO.execute(() -> {
            String failure = "";
            try {
                BufferedImage image = decode(bytes);
                Files.createDirectories(root.resolve(owner.toString()));
                long used;
                try (var files = Files.list(root.resolve(owner.toString()))) { used = files.mapToLong(path -> { try { return Files.size(path); } catch (IOException e) { return QUOTA; } }).sum(); }
                if (used + bytes.length * 2L > QUOTA) throw new IOException("相册存储额度已用完");
                Path file = root.resolve(owner.toString()).resolve(packet.id() + ".jpg");
                Path temp = file.resolveSibling(file.getFileName() + ".part");
                Files.write(temp, bytes);
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
                ImageIO.write(thumbnail(image), "jpg", file.resolveSibling(packet.id() + "-thumb.jpg").toFile());
            } catch (Exception error) { failure = error instanceof IOException ? error.getMessage() : "图片无效"; }
            String error = failure;
            player.server.execute(() -> {
                busy.remove(owner);
                if (error.isEmpty()) { data.photoOwners.put(packet.id(), owner.toString()); data.touch(); }
                if (!player.hasDisconnected()) reply(player, packet.id(), error.isEmpty(), error.isEmpty() ? "图片已上传" : error);
            });
        });
    }

    static boolean validId(String id) { try { return UUID.fromString(id).toString().equals(id); } catch (RuntimeException ignored) { return false; } }

    static BufferedImage decode(byte[] bytes) throws IOException {
        if (bytes.length == 0 || bytes.length > PhonePhotoPayload.MAX_BYTES) throw new IOException("图片过大");
        try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IOException("图片格式无效");
            var reader = readers.next();
            try {
                reader.setInput(input);
                if (!reader.getFormatName().equalsIgnoreCase("JPEG") || reader.getWidth(0) > 1600 || reader.getHeight(0) > 1600) throw new IOException("图片尺寸或格式无效");
                return reader.read(0);
            } finally { reader.dispose(); }
        }
    }

    static BufferedImage thumbnail(BufferedImage image) {
        double scale = Math.min(1, 256.0 / Math.max(image.getWidth(), image.getHeight()));
        BufferedImage thumb = new BufferedImage(Math.max(1, (int)(image.getWidth() * scale)), Math.max(1, (int)(image.getHeight() * scale)), BufferedImage.TYPE_INT_RGB);
        var graphics = thumb.createGraphics();
        graphics.drawImage(image, 0, 0, thumb.getWidth(), thumb.getHeight(), null); graphics.dispose();
        return thumb;
    }

    static boolean allowed(PhoneData data, PhoneData.Profile self, String id) {
        if (self.id.equals(data.photoOwners.get(id))) return true;
        if (data.conversations.stream().anyMatch(m -> m.kind().equals("image") && m.extra().equals(id) && m.visible(data, self))) return true;
        return data.moments.stream().anyMatch(m -> m.photo.equals(id) && (m.author.equalsIgnoreCase(self.wechat) || self.friends.stream().anyMatch(f -> f.equalsIgnoreCase(m.author))));
    }

    private static void download(ServerPlayer player, PhonePhotoPayload packet) {
        PhoneData data = PhoneData.get(player);
        if (!PhoneWifi.online(player) || !allowed(data, data.ensure(player), packet.id())) return;
        long now = System.currentTimeMillis();
        if (now - downloads.getOrDefault(player.getUUID(), 0L) < 100) return;
        downloads.put(player.getUUID(), now);
        String owner = data.photoOwners.get(packet.id());
        if (owner == null) return;
        boolean thumb = packet.index() == -1;
        Path file = root(player).resolve(owner).resolve(packet.id() + (thumb ? "-thumb" : "") + ".jpg");
        IO.execute(() -> {
            try {
                byte[] bytes = Files.readAllBytes(file);
                if (bytes.length > PhonePhotoPayload.MAX_BYTES) return;
                int total = (bytes.length + PhonePhotoPayload.CHUNK - 1) / PhonePhotoPayload.CHUNK;
                player.server.execute(() -> {
                    if (player.hasDisconnected() || !allowed(data, data.ensure(player), packet.id())) return;
                    for (int i = 0; i < total; i++) PacketDistributor.sendToPlayer(player, new PhonePhotoPayload(packet.id(), i, thumb ? -total : total,
                            Arrays.copyOfRange(bytes, i * PhonePhotoPayload.CHUNK, Math.min(bytes.length, (i + 1) * PhonePhotoPayload.CHUNK))));
                });
            } catch (IOException ignored) { }
        });
    }

    private static Path root(ServerPlayer player) { return player.server.getWorldPath(LevelResource.ROOT).resolve("data/lotm_phone_photos"); }
    private static void reply(ServerPlayer player, String id, boolean ok, String message) {
        JsonObject reply = new JsonObject(); reply.addProperty("id", id); reply.addProperty("ok", ok); reply.addProperty("message", message);
        JsonObject packet = new JsonObject(); packet.addProperty("version", 2); packet.addProperty("section", "photoTransfer"); packet.add("value", reply);
        PacketDistributor.sendToPlayer(player, new PhonePayloads.PhoneStatePayload(packet.toString()));
    }
}
