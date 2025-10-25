package com.example.commonlib.tracing;

import org.slf4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class RequestSpan {
    private final String spanId;
    private final Instant startTime;

    private RequestSpan() {
        this.spanId = UUID.randomUUID().toString();
        this.startTime = Instant.now();
    }

    public static RequestSpan start() {
        return new RequestSpan();
    }

    public String getSpanId() {
        return spanId;
    }

    public void logStart(Logger log, String url) {
        log.info("[SPAN {}] Started call to {} at {}", spanId, url, startTime);
    }

    public void logRetry(Logger log, int attempt, String url) {
        log.info("[SPAN {}] Retry attempt {} for {}", spanId, attempt, url);
    }

    public void logSuccess(Logger log, String url) {
        long durationMs = Duration.between(startTime, Instant.now()).toMillis();
        log.info("[SPAN {}] Successfully completed call to {} in {} ms", spanId, url, durationMs);
    }

    public void logFailure(Logger log, String url, Exception ex) {
        long durationMs = Duration.between(startTime, Instant.now()).toMillis();
        log.error("[SPAN {}] Failed call to {} after {} ms - Error: {}", spanId, url, durationMs, ex.getMessage());
    }
}
