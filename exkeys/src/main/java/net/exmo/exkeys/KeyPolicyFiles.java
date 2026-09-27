package net.exmo.exkeys;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class KeyPolicyFiles {
    private KeyPolicyFiles() {}

    public static String read(Path path) throws IOException {
        if (path == null || !Files.isRegularFile(path)) return "";
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    public static boolean write(Path path, String json) {
        if (path == null || json == null) return false;
        try {
            Path parent = path.getParent();
            if (parent != null) Files.createDirectories(parent);
            Path temp = path.resolveSibling(path.getFileName() + ".tmp");
            Files.writeString(temp, json, StandardCharsets.UTF_8);
            try {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException ex) {
            return false;
        }
    }
}
