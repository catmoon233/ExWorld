package net.exmo.exphone;

/** Face-value rules for ATM paper gold. No Minecraft runtime. */
public final class BanknoteHarness {
    public static void main(String[] args) {
        check(Banknotes.valid(1) && Banknotes.valid(5) && Banknotes.valid(10)
                && Banknotes.valid(20) && Banknotes.valid(50) && Banknotes.valid(100), "faces");
        check(!Banknotes.valid(2) && !Banknotes.valid(0) && !Banknotes.valid(-1), "rejected faces");
        check(Banknotes.cost(100, 3) == 300, "cost");
        check(Banknotes.cost(2, 1) == -1, "bad face cost");
        check(Banknotes.cost(10, 0) == -1, "zero count");
        check(Banknotes.cost(1, Banknotes.MAX_COUNT) == Banknotes.MAX_COUNT, "inventory cap");
        check(Banknotes.cost(1, Banknotes.MAX_COUNT + 1) == -1, "over cap");
        System.out.println("banknote rules ok");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
