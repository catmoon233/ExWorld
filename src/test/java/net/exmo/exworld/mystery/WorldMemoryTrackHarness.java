package net.exmo.exworld.mystery;

import net.exmo.exworld.mystery.client.WorldMemoryTrack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Headless round-trip check for the local world timeline format. */
public final class WorldMemoryTrackHarness {
    public static void main(String[] args) throws Exception {
        Path file = Files.createTempFile("exworld-memory-", ".xwr");
        try {
            var actor = new WorldMemoryTrack.Actor(42, "minecraft:zombie", 4, 5, 6, 20, 0);
            try (var writer = WorldMemoryTrack.open(file)) {
                writer.write(new WorldMemoryTrack.Frame(0, "minecraft:overworld", 0, 64, 0,
                        170, 0, 1000, List.of(actor)));
                writer.write(new WorldMemoryTrack.Frame(100, "minecraft:overworld", 10, 64, 0,
                        -170, 0, 1002, List.of(actor)));
                writer.write(new WorldMemoryTrack.Frame(200, "minecraft:the_nether", 900, 64, 0,
                        0, 0, 1004, List.of()));
            }
            var frames = WorldMemoryTrack.read(file);
            if (frames.size() != 3 || !frames.getFirst().actors().equals(List.of(actor)))
                throw new AssertionError("World actors did not survive round-trip");
            var middle = WorldMemoryTrack.at(frames, 50);
            if (Math.abs(middle.x() - 5) > .001 || Math.abs(Math.abs(middle.yaw()) - 180) > .001)
                throw new AssertionError("First-person position or wrapped rotation interpolated incorrectly");
            if (WorldMemoryTrack.at(frames, 150).x() != 10)
                throw new AssertionError("Camera must not interpolate across dimensions");
            System.out.println("WORLD_MEMORY_TRACK_OK");
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
