package ru.bsh.guarantee.balancing;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CircuitBreakerConfiguration {

    private Long failureThreshold;
    private Long timeoutMs;
}
