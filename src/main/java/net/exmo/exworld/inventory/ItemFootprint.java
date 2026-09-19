package net.exmo.exworld.inventory;

/** Rectangular inventory occupancy in grid cells. */
public record ItemFootprint(int width, int height) {
    public static final ItemFootprint UNIT = new ItemFootprint(1, 1);

    public ItemFootprint {
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException("Item footprint must be positive");
        }
    }

    public static ItemFootprint of(int width, int height) {
        return new ItemFootprint(width, height);
    }

    public int area() {
        return width * height;
    }

    public boolean unit() {
        return width == 1 && height == 1;
    }

    public boolean square() {
        return width == height;
    }

    public ItemFootprint rotated() {
        return square() ? this : new ItemFootprint(height, width);
    }

    public String token() {
        return width + "x" + height;
    }

    public static ItemFootprint parse(String token) {
        if (token == null || token.isBlank()) return UNIT;
        String value = token.trim().toLowerCase().replace('*', 'x');
        int split = value.indexOf('x');
        if (split <= 0) return UNIT;
        try {
            int width = Integer.parseInt(value.substring(0, split));
            int height = Integer.parseInt(value.substring(split + 1));
            return new ItemFootprint(Math.max(1, width), Math.max(1, height));
        } catch (NumberFormatException ignored) {
            return UNIT;
        }
    }
}
