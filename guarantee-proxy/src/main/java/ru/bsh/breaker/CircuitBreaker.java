package ru.bsh.breaker;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class CircuitBreaker {

    public enum State {
        CLOSED, OPEN, HALF_OPEN
    }

    private final Long failureThreshold;
    private final Long timeoutMs;

    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private volatile long lastFailureTime = 0L;

    public CircuitBreaker(Long failureThreshold, Long timeoutMs) {
        this.failureThreshold = failureThreshold;
        this.timeoutMs = timeoutMs;
    }

    public Boolean allowRequest() {
        var currentState = state.get();

        switch (currentState) {
            case CLOSED -> {
                return true;
            }
            case OPEN -> {
                var now = System.currentTimeMillis();
                if (now - lastFailureTime >= timeoutMs) {
                    return state.compareAndSet(State.OPEN, State.HALF_OPEN);
                }
                return false;
            }
            case HALF_OPEN -> {
                return false;
            }
            default -> {
                log.error("Получено неизвестное состояние в Circuit Breaker");
                return false;
            }
        }
    }

    public void onSuccess() {
        failureCount.set(0);
        state.set(State.CLOSED);
    }

    public void onFailure() {
        lastFailureTime = System.currentTimeMillis();

        var currentState = state.get();
        if (currentState == State.CLOSED) {
            int count = failureCount.incrementAndGet();
            if (count >= failureThreshold) {
                state.compareAndSet(State.CLOSED, State.OPEN);
            }
        } else if (currentState == State.HALF_OPEN) {
            state.set(State.OPEN);
        }
    }
}