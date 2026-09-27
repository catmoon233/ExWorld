package net.exmo.exphone;

import java.math.*;

final class PhoneCalculator {
    String display = "0";
    private BigDecimal accumulator;
    private String operation = "";
    private boolean fresh = true;
    void press(String key) {
        if (key.equals("C")) { display = "0"; accumulator = null; operation = ""; fresh = true; return; }
        if (key.equals("⌫")) { display = display.length() < 2 ? "0" : display.substring(0, display.length() - 1); return; }
        if ("+−×÷=".contains(key)) {
            try {
                BigDecimal value = new BigDecimal(display);
                if (accumulator != null && !operation.isEmpty() && !fresh) {
                    value = switch (operation) {
                        case "+" -> accumulator.add(value);
                        case "−" -> accumulator.subtract(value);
                        case "×" -> accumulator.multiply(value);
                        default -> accumulator.divide(value, MathContext.DECIMAL64);
                    };
                }
                display = value.stripTrailingZeros().toPlainString();
                accumulator = value; operation = key.equals("=") ? "" : key; fresh = true;
            } catch (ArithmeticException | NumberFormatException error) { display = "错误"; accumulator = null; operation = ""; fresh = true; }
            return;
        }
        if (fresh || display.equals("0")) { display = key.equals(".") ? "0." : key; fresh = false; }
        else if (display.length() < 16 && !(key.equals(".") && display.contains("."))) display += key;
    }
}
