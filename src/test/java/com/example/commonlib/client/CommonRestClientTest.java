package com.example.commonlib.client;

import com.example.commonlib.config.CircuitBreakerProperties;
import com.example.commonlib.config.RestClientProperties;
import com.example.commonlib.config.RetryProperties;
import com.example.commonlib.exception.RemoteServiceException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommonRestClientTest {

    private CommonRestClient client;
    private HttpServer server;
    private static final int PORT = 8085;

    @BeforeEach
    void setup() throws IOException {
        // Start a tiny HTTP server for test
        server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/fail", this::handleFailRequest);
        server.createContext("/success", this::handleSuccessRequest);
        server.start();

        // Configure client properties
        RestClientProperties props = new RestClientProperties();
        props.setConnectionTimeout(1000);
        props.setReadTimeout(1000);

        // Enable retry configuration
        RetryProperties retryProps = new RetryProperties();
        retryProps.setMaxAttempts(3);
        retryProps.setBaseDelayMs(200);
        retryProps.setMaxDelayMs(2000);
        retryProps.setJitterFactor(0.2);
        props.setRetry(retryProps);

        // Enable circuit breaker configuration
        CircuitBreakerProperties cbProps = new CircuitBreakerProperties();
        cbProps.setFailureThreshold(3);
        cbProps.setOpenStateDuration(2);
        props.setCircuitBreaker(cbProps);

        client = new CommonRestClient(props);
    }

    @AfterEach
    void tearDown() {
        if (server != null) server.stop(0);
    }

    private void handleFailRequest(HttpExchange exchange) throws IOException {
        String response = "Simulated failure";
        exchange.sendResponseHeaders(500, response.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }

    private void handleSuccessRequest(HttpExchange exchange) throws IOException {
        String response = "OK";
        exchange.sendResponseHeaders(200, response.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }

    @Test
    void testCircuitBreakerOpensAfterFailures() {
        String url = "http://localhost:" + PORT + "/fail";

        // Cause consecutive failures to trigger circuit breaker
        for (int i = 0; i < 3; i++) {
            assertThrows(RemoteServiceException.class, () -> client.get(url, String.class));
        }

        // Now circuit breaker should open
        RemoteServiceException exception = assertThrows(RemoteServiceException.class,
                () -> client.get(url, String.class));

        assertTrue(exception.getErrorResponse().getMessage().contains("Circuit breaker is open"),
                "Circuit breaker should open after repeated failures");
    }

    @Test
    void testCircuitBreakerRecoversAfterDelay() throws Exception {
        String failUrl = "http://localhost:" + PORT + "/fail";
        String successUrl = "http://localhost:" + PORT + "/success";

        // Trip the circuit breaker
        for (int i = 0; i < 3; i++) {
            assertThrows(RemoteServiceException.class, () -> client.get(failUrl, String.class));
        }

        // Immediately calling again should skip call
        assertThrows(RemoteServiceException.class, () -> client.get(failUrl, String.class));

        // Wait for circuit breaker cool-down (2 seconds as per config)
        Thread.sleep(2500);

        // Should now allow call and succeed
        String response = client.get(successUrl, String.class);
        assertEquals("OK", response);
    }

    @Test
    void testRetryExecutes() {
        String url = "http://localhost:" + PORT + "/fail";
        long start = System.currentTimeMillis();

        assertThrows(RemoteServiceException.class, () -> client.get(url, String.class));

        long duration = System.currentTimeMillis() - start;

        // Expect at least 3 attempts with delay
        assertTrue(duration >= 400, "Should have retried with delay between attempts");
    }

    @Test
    void testNoRetryNoCircuitBreaker() {
        // Setup client with no retry or circuit breaker
        RestClientProperties props = new RestClientProperties();
        props.setConnectionTimeout(1000);
        props.setReadTimeout(1000);

        CommonRestClient simpleClient = new CommonRestClient(props);

        String failUrl = "http://localhost:" + PORT + "/fail";

        long start = System.currentTimeMillis();
        RemoteServiceException ex = assertThrows(RemoteServiceException.class,
                () -> simpleClient.get(failUrl, String.class));
        long duration = System.currentTimeMillis() - start;

        // Since no retry, duration should be very short
        assertTrue(duration < 200, "No retry: should fail fast");
        assertFalse(ex.getErrorResponse().getMessage().contains("Circuit breaker"),
                "Circuit breaker should not be applied");
    }

    @Test
    void testRetryOnly() {
        // Setup client with only retry
        RestClientProperties props = new RestClientProperties();
        props.setConnectionTimeout(1000);
        props.setReadTimeout(1000);

        RetryProperties retryProps = new RetryProperties();
        retryProps.setMaxAttempts(3);
        retryProps.setBaseDelayMs(200);
        props.setRetry(retryProps);

        CommonRestClient retryClient = new CommonRestClient(props);

        String failUrl = "http://localhost:" + PORT + "/fail";

        long start = System.currentTimeMillis();
        assertThrows(RemoteServiceException.class, () -> retryClient.get(failUrl, String.class));
        long duration = System.currentTimeMillis() - start;

        // Expect total duration >= 2 retries × 200ms delay
        assertTrue(duration >= 400, "Retry should apply delay between attempts");
    }

    @Test
    void testCircuitBreakerOnly() throws InterruptedException {
        // Setup client with only circuit breaker
        RestClientProperties props = new RestClientProperties();
        props.setConnectionTimeout(1000);
        props.setReadTimeout(1000);

        CircuitBreakerProperties cbProps = new CircuitBreakerProperties();
        cbProps.setFailureThreshold(2);
        cbProps.setOpenStateDuration(1); // 1 second
        props.setCircuitBreaker(cbProps);

        CommonRestClient cbClient = new CommonRestClient(props);

        String failUrl = "http://localhost:" + PORT + "/fail";

        // Trigger failures to open the circuit breaker
        for (int i = 0; i < 2; i++) {
            assertThrows(RemoteServiceException.class, () -> cbClient.get(failUrl, String.class));
        }

        // Circuit breaker should now open
        RemoteServiceException ex = assertThrows(RemoteServiceException.class,
                () -> cbClient.get(failUrl, String.class));
        assertTrue(ex.getErrorResponse().getMessage().contains("Circuit breaker is open"));

        // Wait for recovery
        Thread.sleep(1200);

        // Should allow request again
        RemoteServiceException ex2 = assertThrows(RemoteServiceException.class,
                () -> cbClient.get(failUrl, String.class));
        // Circuit breaker logic resets after open duration
        assertFalse(ex2.getErrorResponse().getMessage().contains("Circuit breaker is open"));
    }
}