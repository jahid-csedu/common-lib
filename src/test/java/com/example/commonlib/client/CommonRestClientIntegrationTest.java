package com.example.commonlib.client;

import com.example.commonlib.config.CircuitBreakerProperties;
import com.example.commonlib.config.RestClientProperties;
import com.example.commonlib.config.RetryProperties;
import com.example.commonlib.exception.RemoteServiceException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class CommonRestClientIntegrationTest {

    private static MockWebServer mockServer;
    private CommonRestClient client;

    @BeforeEach
    void setup() throws IOException {
        mockServer = new MockWebServer();
        mockServer.start();

        RestClientProperties props = new RestClientProperties();
        props.setConnectionTimeout(1000);
        props.setReadTimeout(1000);

        RetryProperties retryProps = new RetryProperties();
        retryProps.setMaxAttempts(3);
        retryProps.setBaseDelayMs(200);
        retryProps.setMaxDelayMs(1000);
        props.setRetry(retryProps);

        CircuitBreakerProperties cbProps = new CircuitBreakerProperties();
        cbProps.setFailureThreshold(2);
        cbProps.setOpenDurationMs(2000);
        props.setCircuitBreaker(cbProps);

        client = new CommonRestClient(props);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockServer.shutdown();
    }

    @Test
    void testSuccessfulGetRequest() {
        mockServer.enqueue(new MockResponse()
                .setBody("Hello World")
                .setResponseCode(200));

        String url = mockServer.url("/success").toString();

        String response = client.get(url, String.class);

        assertEquals("Hello World", response);
    }

    @Test
    void testRetryMechanism() {
        mockServer.enqueue(new MockResponse().setResponseCode(500));
        mockServer.enqueue(new MockResponse().setResponseCode(500));
        mockServer.enqueue(new MockResponse().setBody("Recovered!").setResponseCode(200));

        String url = mockServer.url("/retry").toString();

        try {
            String result = client.get(url, String.class);
            assertEquals("Recovered!", result);
        } catch (Exception e) {
            fail("Should have succeeded after retries");
        }
    }

    @Test
    void testCircuitBreakerOpensAfterFailures() {
        mockServer.enqueue(new MockResponse().setResponseCode(500));
        mockServer.enqueue(new MockResponse().setResponseCode(500));

        String url = mockServer.url("/fail").toString();

        assertThrows(RemoteServiceException.class, () -> client.get(url, String.class));

        // Second call will trigger circuit open
        assertThrows(RemoteServiceException.class, () -> client.get(url, String.class));

        // Circuit breaker should be open now
        RemoteServiceException openException = assertThrows(RemoteServiceException.class, () -> client.get(url, String.class));
        assertTrue(openException.getErrorResponse().getMessage().contains("Circuit breaker is open"));
    }

    @Test
    void testTimeoutHandling() {
        mockServer.enqueue(new MockResponse()
                .setBody("This will timeout")
                .setResponseCode(200)
                .setBodyDelay(2, TimeUnit.SECONDS));

        String url = mockServer.url("/timeout").toString();

        assertThrows(RemoteServiceException.class, () -> client.get(url, String.class));
    }

    @Test
    void testSuccessfulPostRequest() {
        mockServer.enqueue(new MockResponse()
                .setBody("Hello World")
                .setResponseCode(200));

        String url = mockServer.url("/success").toString();

        String response = client.post(url, "Test Request", String.class);

        assertEquals("Hello World", response);
    }
}
