package ru.bsh.guarantee.balancing;

import lombok.Data;

@Data
public class CircuitBreakerConfiguration {

    private Long failureThreshold;
    private Long timeoutMs;
}
