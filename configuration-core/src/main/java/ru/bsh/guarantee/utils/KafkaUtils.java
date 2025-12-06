package ru.bsh.guarantee.utils;

public class KafkaUtils {

    public static Throwable getExceptionCause(Throwable throwable) {
        var current = throwable;

        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }
}
