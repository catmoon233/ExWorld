package net.exmo.exkeys.server;

import net.exmo.exkeys.ExKeys;
import net.exmo.exkeys.KeyPolicy;
import net.exmo.exkeys.KeyPolicyFiles;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Path;

/** Authoritative policy for a dedicated server or an open LAN world. */
public final class KeyPolicyStore {
    private static final Object LOCK = new Object();
    private static KeyPolicy current = KeyPolicy.EMPTY;
    private static boolean loaded;

    private KeyPolicyStore() {}

    public static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve("exkeys-policy.json");
    }

    public static KeyPolicy get() {
        synchronized (LOCK) {
            if (!loaded) load();
            return current;
        }
    }

    public static void load() {
        synchronized (LOCK) {
            loaded = true;
            try {
                KeyPolicy.ParseResult parsed = KeyPolicy.parse(KeyPolicyFiles.read(path()), false);
                if (!parsed.ok()) {
                    ExKeys.LOGGER.error("Ignoring unreadable ExKeys policy at {}: {}", path(), parsed.error());
                    current = KeyPolicy.EMPTY;
                    return;
                }
                current = parsed.policy();
            } catch (IOException ex) {
                ExKeys.LOGGER.error("Failed to read ExKeys policy at {}", path(), ex);
                current = KeyPolicy.EMPTY;
            }
        }
    }

    public static boolean replace(KeyPolicy policy) {
        KeyPolicy next = policy == null ? KeyPolicy.EMPTY : policy;
        synchronized (LOCK) {
            if (!KeyPolicyFiles.write(path(), next.toJson())) return false;
            current = next;
            loaded = true;
            return true;
        }
    }
}
