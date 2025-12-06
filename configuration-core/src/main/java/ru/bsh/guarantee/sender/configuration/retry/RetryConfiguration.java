package ru.bsh.guarantee.sender.configuration.retry;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import ru.bsh.guarantee.exception.InternalGuaranteeException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@Slf4j
public class RetryConfiguration {

    private Integer maxAttempts;
    private Map<Class<? extends Throwable>, Boolean> exceptionsToRetry = new HashMap<>();
    private Map<String, Boolean> exceptionsMap;
    private Integer initialInterval;
    private Double intervalMultiplier;
    private Integer maxInterval;

    public Map<Class<? extends Throwable>, Boolean> getExceptionsToRetry() {
        if (!exceptionsToRetry.isEmpty()) {
            return exceptionsToRetry;
        }
        exceptionsToRetry = exceptionsMap.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> {
                            try {
                                return (Class<? extends Throwable>) Class.forName(e.getKey());
                            } catch (ClassNotFoundException ex) {
                                throw new InternalGuaranteeException(
                                        String.format("Ошибка в конфигурации исключений" +
                                                " для повторного Http запроса %s", ex.getMessage())
                                );
                            }
                        },
                        Map.Entry::getValue
                ));
        return exceptionsToRetry;
    }
}

