package net.exmo.exphone;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.storage.LevelResource;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

/** A local, world/account-scoped screen PIN. It never travels over the game network. */
final class PhoneLock {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static Path file;
    private static byte[] salt, hash;
    private static boolean unlocked;
    private static int failures;
    private static long retryAt;

    static void open() {
        Minecraft mc = Minecraft.getInstance();
        String world = mc.hasSingleplayerServer()
                ? mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().toString()
                : mc.getCurrentServer() == null ? "unknown" : mc.getCurrentServer().ip;
        String scope = UUID.nameUUIDFromBytes(world.getBytes(StandardCharsets.UTF_8)).toString();
        Path next = mc.gameDirectory.toPath().resolve("phone-lock").resolve(scope)
                .resolve(mc.getUser().getProfileId() + ".json");
        file = next;
        salt = hash = null;
        unlocked = false;
        failures = 0;
        retryAt = 0;
        if (!Files.isRegularFile(next)) return;
        try {
            JsonObject saved = JsonParser.parseString(Files.readString(next)).getAsJsonObject();
            salt = Base64.getDecoder().decode(saved.get("salt").getAsString());
            hash = Base64.getDecoder().decode(saved.get("hash").getAsString());
        } catch (Exception error) {
            salt = hash = null;
            PhoneApricity.notice("锁屏密码读取失败，请重新设置");
        }
    }

    static boolean hasPin() { return hash != null; }
    static boolean unlocked() { return unlocked || !hasPin(); }
    static void lock() { unlocked = false; }

    static String unlock(String pin) {
        if (!hasPin()) { unlocked = true; return ""; }
        if (System.currentTimeMillis() < retryAt) return "尝试过多，请稍后再试";
        if (!matches(pin)) {
            failures++;
            if (failures >= 5) { failures = 0; retryAt = System.currentTimeMillis() + 30_000; }
            return "密码不正确";
        }
        failures = 0;
        retryAt = 0;
        unlocked = true;
        return "";
    }

    static String change(String oldPin, String newPin, String confirmation) {
        if (hasPin() && !matches(oldPin)) return "当前密码不正确";
        if (!newPin.equals(confirmation)) return "两次输入的新密码不一致";
        if (!newPin.matches("[0-9]{4,8}")) return "请设置 4–8 位数字密码";
        byte[] nextSalt = new byte[16];
        RANDOM.nextBytes(nextSalt);
        byte[] nextHash = derive(newPin, nextSalt);
        if (nextHash == null) return "密码设置失败";
        JsonObject saved = new JsonObject();
        saved.addProperty("version", 1);
        saved.addProperty("salt", Base64.getEncoder().encodeToString(nextSalt));
        saved.addProperty("hash", Base64.getEncoder().encodeToString(nextHash));
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, saved.toString());
            salt = nextSalt; hash = nextHash; unlocked = true;
            return "";
        } catch (IOException error) { return "密码保存失败：" + error.getMessage(); }
    }

    static String remove(String oldPin) {
        if (!hasPin()) return "尚未设置密码";
        if (!matches(oldPin)) return "当前密码不正确";
        try {
            Files.deleteIfExists(file);
            salt = hash = null;
            unlocked = true;
            return "";
        } catch (IOException error) { return "密码移除失败：" + error.getMessage(); }
    }

    private static boolean matches(String pin) {
        byte[] derived = derive(pin, salt);
        return derived != null && MessageDigest.isEqual(derived, hash);
    }

    private static byte[] derive(String pin, byte[] salt) {
        if (salt == null) return null;
        PBEKeySpec spec = new PBEKeySpec(pin.toCharArray(), salt, 60_000, 256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        catch (Exception ignored) { return null; }
        finally { spec.clearPassword(); }
    }
}
