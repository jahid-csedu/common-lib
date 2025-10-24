package com.example.commonlib.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rest.client")
public class RestClientProperties {

    /**
     * Connection timeout in milliseconds
     */
    private int connectionTimeout = 5000;

    /**
     * Read timeout in milliseconds
     */
    private int readTimeout = 5000;

    private RetryProperties retryProperties = new RetryProperties();

    private CircuitBreakerProperties circuitBreakerProperties = new CircuitBreakerProperties();

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public RetryProperties getRetryProperties() {
        return retryProperties;
    }

    public void setRetryProperties(RetryProperties retryProperties) {
        this.retryProperties = retryProperties;
    }

    public CircuitBreakerProperties getCircuitBreakerProperties() {
        return circuitBreakerProperties;
    }

    public void setCircuitBreakerProperties(CircuitBreakerProperties circuitBreakerProperties) {
        this.circuitBreakerProperties = circuitBreakerProperties;
    }
}
