package net.exmo.exphone.api;

import java.util.List;
import net.minecraft.server.MinecraftServer;

public final class PhoneNpcs {
    private static volatile PhoneNpcSource source = new PhoneNpcSource() {
        @Override public List<PhoneNpc> list(MinecraftServer server) { return List.of(); }
        @Override public PhoneNpc find(MinecraftServer server, String id) { return null; }
    };

    private PhoneNpcs() {}

    public static void connect(PhoneNpcSource next) {
        source = next == null ? new PhoneNpcSource() {
            @Override public List<PhoneNpc> list(MinecraftServer server) { return List.of(); }
            @Override public PhoneNpc find(MinecraftServer server, String id) { return null; }
        } : next;
    }

    public static List<PhoneNpc> list(MinecraftServer server) {
        try {
            List<PhoneNpc> found = source.list(server);
            return found == null ? List.of() : found;
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    public static PhoneNpc find(MinecraftServer server, String id) {
        if (id == null || id.isBlank()) return null;
        try {
            return source.find(server, id);
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
