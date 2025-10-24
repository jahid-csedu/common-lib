package com.example.commonlib.config;

public class RetryProperties {

    /**
     * Maximum number of retry attempts
     */
    private int maxAttempts = 3;

    /**
     * Base delay between retries in milliseconds
     */
    private long baseDelayMs = 200;

    /**
     * Maximum backoff delay in milliseconds
     */
    private long maxDelayMs = 2000;

    /**
     * Jitter percentage (0.2 = ±20% variation)
     */
    private double jitterFactor = 0.2;

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public long getBaseDelayMs() {
        return baseDelayMs;
    }

    public void setBaseDelayMs(long baseDelayMs) {
        this.baseDelayMs = baseDelayMs;
    }

    public long getMaxDelayMs() {
        return maxDelayMs;
    }

    public void setMaxDelayMs(long maxDelayMs) {
        this.maxDelayMs = maxDelayMs;
    }

    public double getJitterFactor() {
        return jitterFactor;
    }

    public void setJitterFactor(double jitterFactor) {
        this.jitterFactor = jitterFactor;
    }
}
