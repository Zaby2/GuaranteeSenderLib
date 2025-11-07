package ru.bsh.breaker;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

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

        if (currentState == State.CLOSED) {
            return true;
        }

        if (currentState == State.OPEN) {
            var now = System.currentTimeMillis();
            if (now - lastFailureTime >= timeoutMs) {
                if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
                    return true;
                }
            }
            return false;
        }

        if (currentState == State.HALF_OPEN) {
            return false;
        }

        return false;
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