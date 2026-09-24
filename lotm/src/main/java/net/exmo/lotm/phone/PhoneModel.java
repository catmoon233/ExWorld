package net.exmo.lotm.phone;

import java.util.Locale;
import java.util.Optional;

/**
 * Physical phone only. This type has no hotspot, password, coverage, or owner fields.
 * A complete instance always has both fields; there is no default brand or color.
 */
public record PhoneModel(Brand brand, Color color) {
    public PhoneModel {
        if (brand == null) {
            throw new IllegalArgumentException("phone brand is required");
        }
        if (color == null) {
            throw new IllegalArgumentException("phone color is required");
        }
    }

    /** 型号。只能是这三项，没有第四项，也不能为空。 */
    public enum Brand {
        OPPO("OPPO"),
        APPLE("苹果"),
        XIAOMI("小米");

        private final String label;

        Brand(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        public static Optional<Brand> parse(String raw) {
            if (raw == null) return Optional.empty();
            return switch (raw.trim().toUpperCase(Locale.ROOT)) {
                case "OPPO" -> Optional.of(OPPO);
                case "APPLE", "苹果" -> Optional.of(APPLE);
                case "XIAOMI", "小米" -> Optional.of(XIAOMI);
                default -> Optional.empty();
            };
        }
    }

    /** 机身颜色。只能是这六项，没有第七项，也不能为空。 */
    public enum Color {
        RED("红"),
        ORANGE("橙"),
        YELLOW("黄"),
        GREEN("绿"),
        BLUE("蓝"),
        PURPLE("紫");

        private final String label;

        Color(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        public static Optional<Color> parse(String raw) {
            if (raw == null) return Optional.empty();
            return switch (raw.trim().toUpperCase(Locale.ROOT)) {
                case "RED", "红" -> Optional.of(RED);
                case "ORANGE", "橙" -> Optional.of(ORANGE);
                case "YELLOW", "黄" -> Optional.of(YELLOW);
                case "GREEN", "绿" -> Optional.of(GREEN);
                case "BLUE", "蓝" -> Optional.of(BLUE);
                case "PURPLE", "紫" -> Optional.of(PURPLE);
                default -> Optional.empty();
            };
        }
    }

    public static PhoneModel require(String brand, String color) {
        Brand parsedBrand = Brand.parse(brand)
                .orElseThrow(() -> new IllegalArgumentException("phone brand must be OPPO, APPLE, or XIAOMI"));
        Color parsedColor = Color.parse(color)
                .orElseThrow(() -> new IllegalArgumentException("phone color must be RED, ORANGE, YELLOW, GREEN, BLUE, or PURPLE"));
        return new PhoneModel(parsedBrand, parsedColor);
    }
}
