package ru.bsh.guarantee.sender.configuration.retry;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class RetryConfiguration {

    private Integer maxAttempts;
    private Map<Class<? extends Throwable>, Boolean> exceptionsToRetry = new HashMap<>();
    private Integer initialInterval;
    private Double intervalMultiplier;
    private Integer maxInterval;
}
