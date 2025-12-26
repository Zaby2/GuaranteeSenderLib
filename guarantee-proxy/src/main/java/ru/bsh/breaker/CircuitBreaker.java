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
            case CLOSED, HALF_OPEN -> {
                return true;
            }
            case OPEN -> {
                var now = System.currentTimeMillis();
                if (now - lastFailureTime >= timeoutMs) {
                    boolean transitioned = state.compareAndSet(State.OPEN, State.HALF_OPEN);
                    return transitioned;
                }
                return false;
            }
            default -> {
                log.error("Получено неизвестное состояние в Circuit Breaker: {}", currentState);
                return false;
            }
        }
    }

    public void onSuccess() {
        if (state.get() == State.HALF_OPEN) {
            if (state.compareAndSet(State.HALF_OPEN, State.CLOSED)) {
                log.info("Circuit Breaker transitioned from HALF_OPEN to CLOSED after successful request.");
                failureCount.set(0);
            }
        } else {
            failureCount.set(0);
        }
    }

    public void onFailure() {
        lastFailureTime = System.currentTimeMillis();

        var currentState = state.get();
        if (currentState == State.CLOSED) {
            int count = failureCount.incrementAndGet();
            if (count >= failureThreshold && state.compareAndSet(State.CLOSED, State.OPEN)) {
                log.warn("Circuit Breaker transitioned from CLOSED to OPEN after {} failures.", failureThreshold);
            }
        } else if (currentState == State.HALF_OPEN) {
            if (state.compareAndSet(State.HALF_OPEN, State.OPEN)) {
                log.warn("Circuit Breaker transitioned from HALF_OPEN to OPEN after failure.");
            }
        }
    }
}