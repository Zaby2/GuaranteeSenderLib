package ru.bsh.guarantee.sender.configuration.sql;

public class DbContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    public static void set(String db) {
        CURRENT.set(db);
    }

    public static String get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
