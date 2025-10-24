package com.example.commonlib.client;

import com.example.commonlib.config.CircuitBreakerProperties;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

public class CircuitBreaker {

    public enum State {
        CLOSED, // Normal Operation
        OPEN, // Calls are temporarily blocked
        HALF_OPEN // Test state after cool-down
    }

    private final int failureThreshold;
    private final Duration openStateDuration;
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private volatile State state = State.CLOSED;
    private volatile Instant lastFailureTime;

    public CircuitBreaker(CircuitBreakerProperties props) {
        this.failureThreshold = props.getFailureThreshold();
        this.openStateDuration = Duration.ofSeconds(props.getOpenStateDuration());
    }

    public synchronized boolean allowRequest() {
        if (state == State.OPEN) {
            if (Instant.now().isAfter(lastFailureTime.plus(openStateDuration))) {
                state = State.HALF_OPEN;
                return true;
            }
            return false;
        }
        return true;
    }

    public synchronized void recordSuccess() {
        failureCount.set(0);
        state = State.CLOSED;
    }

    public synchronized void recordFailure() {
        failureCount.incrementAndGet();
        lastFailureTime = Instant.now();

        if (failureCount.get() >= failureThreshold) {
            state = State.OPEN;
        }
    }

    public State getState() {
        return state;
    }
}
