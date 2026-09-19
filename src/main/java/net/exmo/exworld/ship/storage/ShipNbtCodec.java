package net.exmo.exworld.ship.storage;

import net.exmo.exworld.ship.model.PartSelection;
import net.exmo.exworld.ship.model.ShipBlock;
import net.exmo.exworld.ship.model.ShipHull;
import net.exmo.exworld.ship.model.ShipPart;
import net.exmo.exworld.ship.model.ShipSlot;
import net.exmo.exworld.ship.model.ShipTemplate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/** Palette + packed-coordinate gzip codec. Independent of Minecraft NBT so tests stay pure Java. */
public final class ShipNbtCodec {
    public static final int MAX_COMPRESSED_BYTES = 1_048_576;
    private static final int VERSION = 1;

    private ShipNbtCodec() {}

    public static byte[] encodeHull(ShipHull hull) { return gzip(output -> writeHull(output, hull)); }
    public static ShipHull decodeHull(byte[] compressed) { return read(compressed, ShipNbtCodec::readHull); }

    public static byte[] encodeTemplate(ShipTemplate template) {
        return gzip(output -> {
            output.writeInt(VERSION);
            output.writeUTF(template.id());
            output.writeUTF(template.name());
            writeHull(output, template.hull());
            output.writeInt(template.parts().size());
            for (ShipPart part : template.parts()) writePart(output, part);
            output.writeInt(template.selection().variantByPart().size());
            for (Map.Entry<String, String> entry : template.selection().variantByPart().entrySet()) {
                output.writeUTF(entry.getKey());
                output.writeUTF(entry.getValue());
            }
        });
    }

    public static ShipTemplate decodeTemplate(byte[] compressed) {
        return read(compressed, input -> {
            int version = bounded(input.readInt(), 1, VERSION, "version");
            if (version != VERSION) throw new IOException("unsupported ship template version " + version);
            String id = input.readUTF();
            String name = input.readUTF();
            ShipHull hull = readHull(input);
            int partCount = bounded(input.readInt(), 0, 256, "part count");
            List<ShipPart> parts = new ArrayList<>(partCount);
            for (int i = 0; i < partCount; i++) parts.add(readPart(input));
            int selectionCount = bounded(input.readInt(), 0, 256, "selection count");
            Map<String, String> selection = new LinkedHashMap<>();
            for (int i = 0; i < selectionCount; i++) selection.put(input.readUTF(), input.readUTF());
            return new ShipTemplate(id, name, hull, parts, new PartSelection(selection));
        });
    }

    private static void writeHull(DataOutputStream output, ShipHull hull) throws IOException {
        output.writeInt(hull.revision());
        output.writeInt(hull.palette().size());
        for (String key : hull.palette()) output.writeUTF(key);
        List<ShipBlock> blocks = hull.blocks();
        output.writeInt(blocks.size());
        for (ShipBlock block : blocks) {
            output.writeInt(block.packed());
            output.writeUTF(block.block());
            output.writeInt(block.slots().size());
            for (ShipSlot slot : block.slots()) {
                output.writeByte(slot.index());
                output.writeUTF(slot.itemId());
                output.writeByte(slot.count());
                output.writeUTF(slot.extra());
            }
        }
    }

    private static ShipHull readHull(DataInputStream input) throws IOException {
        int revision = Math.max(0, input.readInt());
        int paletteSize = bounded(input.readInt(), 0, ShipHull.MAX_BLOCKS, "palette size");
        List<String> palette = new ArrayList<>(paletteSize);
        for (int i = 0; i < paletteSize; i++) palette.add(input.readUTF());
        int count = bounded(input.readInt(), 0, ShipHull.MAX_BLOCKS, "hull size");
        List<ShipBlock> blocks = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int packed = input.readInt();
            String block = input.readUTF();
            if (block.isBlank() && packed >= 0 && packed < paletteSize) block = palette.get(packed);
            int slotCount = bounded(input.readInt(), 0, 256, "slot count");
            List<ShipSlot> slots = new ArrayList<>(slotCount);
            for (int s = 0; s < slotCount; s++) {
                int index = input.readUnsignedByte();
                String item = input.readUTF();
                int amount = input.readUnsignedByte();
                String extra = input.readUTF();
                slots.add(new ShipSlot(index, item, amount, extra));
            }
            int x = packed & 0x7F;
            int y = (packed >>> 7) & 0x7F;
            int z = (packed >>> 14) & 0x7F;
            blocks.add(new ShipBlock(x, y, z, block, slots));
        }
        return ShipHull.of(blocks, revision);
    }

    private static void writePart(DataOutputStream output, ShipPart part) throws IOException {
        output.writeUTF(part.id());
        output.writeUTF(part.name());
        output.writeInt(part.color());
        output.writeDouble(part.speed());
        int[] occupancy = part.occupancy();
        output.writeInt(occupancy.length);
        for (int packed : occupancy) output.writeInt(packed);
        output.writeInt(part.allowedVariants().size());
        for (String variant : part.allowedVariants()) output.writeUTF(variant);
    }

    private static ShipPart readPart(DataInputStream input) throws IOException {
        String id = input.readUTF();
        String name = input.readUTF();
        int color = input.readInt();
        double speed = input.readDouble();
        int occupancyCount = bounded(input.readInt(), 0, ShipHull.MAX_BLOCKS, "occupancy");
        int[] occupancy = new int[occupancyCount];
        for (int i = 0; i < occupancyCount; i++) occupancy[i] = input.readInt();
        int variantCount = bounded(input.readInt(), 0, 256, "variants");
        List<String> variants = new ArrayList<>(variantCount);
        for (int i = 0; i < variantCount; i++) variants.add(input.readUTF());
        return new ShipPart(id, name, color, occupancy, variants, speed);
    }

    private interface Writer { void write(DataOutputStream output) throws IOException; }
    private interface Reader<T> { T read(DataInputStream input) throws IOException; }

    private static byte[] gzip(Writer writer) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(new GZIPOutputStream(bytes))) {
                writer.write(output);
            }
            byte[] compressed = bytes.toByteArray();
            if (compressed.length > MAX_COMPRESSED_BYTES) {
                throw new IllegalArgumentException("compressed ship data exceeds " + MAX_COMPRESSED_BYTES + " bytes");
            }
            return compressed;
        } catch (IOException exception) {
            throw new IllegalStateException("could not encode ship data", exception);
        }
    }

    private static <T> T read(byte[] compressed, Reader<T> reader) {
        if (compressed.length > MAX_COMPRESSED_BYTES) throw new IllegalArgumentException("ship data is too large");
        try (DataInputStream input = new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(compressed)))) {
            return reader.read(input);
        } catch (IOException exception) {
            throw new IllegalArgumentException("could not decode ship data", exception);
        }
    }

    private static int bounded(int value, int minimum, int maximum, String name) throws IOException {
        if (value < minimum || value > maximum) throw new IOException(name + " outside bounds: " + value);
        return value;
    }
}
