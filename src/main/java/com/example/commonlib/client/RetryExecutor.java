package com.example.commonlib.client;

import com.example.commonlib.config.RetryProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.concurrent.Callable;

public class RetryExecutor {
    private static final Logger log = LoggerFactory.getLogger(RetryExecutor.class);

    private final RetryProperties retryProperties;
    private final Random random = new Random();

    public RetryExecutor(RetryProperties retryProperties) {
        this.retryProperties = retryProperties;
    }

    public <T> T executeWithRetry(Callable<T> action) throws Exception {
        int attempts = 0;

        while (true) {
            try {
                return action.call();
            } catch (Exception ex) {
                attempts++;
                if (attempts >= retryProperties.getMaxAttempts()) {
                    log.warn("Max retry exceed");
                    throw ex;
                }

                long delay = computeBackoffDelay(attempts);
                log.debug("Call failed. Retry after {} milliseconds", delay);
                Thread.sleep(delay);
            }
        }
    }

    private long computeBackoffDelay(int attempts) {
        long delay = (long) (retryProperties.getBaseDelayMs() * Math.pow(2, attempts - 1));
        delay = Math.min(delay, retryProperties.getMaxDelayMs());

        double jitter = 1 + (random.nextDouble() * 2 - 1) * retryProperties.getJitterFactor();
        return (long) (delay * jitter);
    }
}
