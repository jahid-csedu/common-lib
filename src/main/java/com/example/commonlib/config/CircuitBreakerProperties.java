package com.example.commonlib.config;

public class CircuitBreakerProperties {
    /**
     * Maximum failure count to make the state OPEN
     */
    private int failureThreshold = 3;

    /**
     * Maximum duration in seconds the circuit will be in OPEN state
     */
    private long openDurationMs = 10;

    public int getFailureThreshold() {
        return failureThreshold;
    }

    public void setFailureThreshold(int failureThreshold) {
        this.failureThreshold = failureThreshold;
    }

    public long getOpenDurationMs() {
        return openDurationMs;
    }

    public void setOpenDurationMs(long openDurationMs) {
        this.openDurationMs = openDurationMs;
    }
}
