package net.exmo.exworld.mystery.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/** Compact, client-local scene timeline. Positions and actors are data, never captured pixels. */
public final class WorldMemoryTrack {
    private static final int MAGIC = 0x58575231; // XWR1
    private static final int MAX_FRAMES = 200_000;
    private static final int MAX_ACTORS = 64;

    public record Actor(int id, String type, double x, double y, double z, float yaw, float pitch) {}
    public record Frame(int millis, String dimension, double x, double y, double z,
                        float yaw, float pitch, long worldTime, List<Actor> actors) {
        public Frame { actors = List.copyOf(actors); }
    }

    private WorldMemoryTrack() {}

    public static Writer open(Path path) throws IOException {
        return new Writer(new DataOutputStream(new GZIPOutputStream(Files.newOutputStream(path,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING), 16 * 1024)));
    }

    public static List<Frame> read(Path path) throws IOException {
        List<Frame> result = new ArrayList<>();
        try (DataInputStream input = new DataInputStream(new GZIPInputStream(Files.newInputStream(path), 16 * 1024))) {
            if (input.readInt() != MAGIC) throw new IOException("Unknown world memory version");
            while (result.size() < MAX_FRAMES) {
                try {
                    int millis = input.readInt();
                    String dimension = input.readUTF();
                    double x = input.readDouble(), y = input.readDouble(), z = input.readDouble();
                    float yaw = input.readFloat(), pitch = input.readFloat();
                    long time = input.readLong();
                    int actorCount = input.readUnsignedByte();
                    if (actorCount > MAX_ACTORS || millis < 0 || (!result.isEmpty() && millis < result.getLast().millis))
                        throw new IOException("Invalid world memory frame");
                    List<Actor> actors = new ArrayList<>(actorCount);
                    for (int i = 0; i < actorCount; i++) actors.add(new Actor(input.readInt(), input.readUTF(),
                            input.readDouble(), input.readDouble(), input.readDouble(), input.readFloat(), input.readFloat()));
                    result.add(new Frame(millis, dimension, x, y, z, yaw, pitch, time, actors));
                } catch (EOFException end) { break; }
            }
        }
        return result;
    }

    public static Frame at(List<Frame> frames, long millis) {
        if (frames.isEmpty()) return null;
        int low = 0, high = frames.size() - 1;
        while (low < high) {
            int middle = (low + high + 1) >>> 1;
            if (frames.get(middle).millis <= millis) low = middle;
            else high = middle - 1;
        }
        Frame first = frames.get(low);
        if (low + 1 >= frames.size()) return first;
        Frame second = frames.get(low + 1);
        if (!first.dimension.equals(second.dimension)) return first;
        double alpha = Math.max(0, Math.min(1, (millis - first.millis) / (double)Math.max(1, second.millis - first.millis)));
        return new Frame((int)millis, first.dimension,
                first.x + (second.x - first.x) * alpha,
                first.y + (second.y - first.y) * alpha,
                first.z + (second.z - first.z) * alpha,
                angle(first.yaw, second.yaw, alpha), angle(first.pitch, second.pitch, alpha),
                first.worldTime, first.actors);
    }

    private static float angle(float start, float end, double alpha) {
        float difference = (end - start + 540) % 360 - 180;
        return (float)(start + difference * alpha);
    }

    public static final class Writer implements AutoCloseable {
        private final DataOutputStream output;
        private int lastMillis = -1;

        private Writer(DataOutputStream output) throws IOException { this.output = output; output.writeInt(MAGIC); }

        public void write(Frame frame) throws IOException {
            if (frame.millis < lastMillis || frame.actors.size() > MAX_ACTORS) throw new IOException("Invalid scene frame");
            output.writeInt(frame.millis); output.writeUTF(frame.dimension);
            output.writeDouble(frame.x); output.writeDouble(frame.y); output.writeDouble(frame.z);
            output.writeFloat(frame.yaw); output.writeFloat(frame.pitch); output.writeLong(frame.worldTime);
            output.writeByte(frame.actors.size());
            for (Actor actor : frame.actors) {
                output.writeInt(actor.id); output.writeUTF(actor.type);
                output.writeDouble(actor.x); output.writeDouble(actor.y); output.writeDouble(actor.z);
                output.writeFloat(actor.yaw); output.writeFloat(actor.pitch);
            }
            lastMillis = frame.millis;
        }

        @Override public void close() throws IOException { output.close(); }
    }
}
