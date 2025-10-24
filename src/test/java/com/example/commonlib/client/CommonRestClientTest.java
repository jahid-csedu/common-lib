package com.example.commonlib.client;

import com.example.commonlib.config.RestClientProperties;
import com.example.commonlib.exception.RemoteServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CommonRestClientTest {

    private CommonRestClient client;

    @BeforeEach
    void setup() {
        RestClientProperties props = new RestClientProperties();
        props.setConnectionTimeout(1000);
        props.setReadTimeout(1000);
        client = new CommonRestClient(props);
    }

    @Test
    void testCircuitBreakerOpensAfterFailures() {
        String failingUrl = "http://localhost:9999/fail";
        for(int i=0; i<3; i++) {
            assertThrows(RemoteServiceException.class, () -> client.get(failingUrl, String.class));
        }

        RemoteServiceException exception = assertThrows(RemoteServiceException.class, () -> client.get(failingUrl, String.class));

        assertTrue(exception.getErrorResponse().getMessage().contains("Circuit breaker is open"));
    }

    @Test
    void testRetryExecutes() {
        String failingUrl = "http://localhost:9999/fail";
        long start = System.currentTimeMillis();

        assertThrows(RemoteServiceException.class, () -> client.get(failingUrl, String.class));

        long duration = System.currentTimeMillis() - start;

        assertTrue(duration >= 200);
    }
}