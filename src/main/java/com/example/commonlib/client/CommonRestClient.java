package com.example.commonlib.client;

import com.example.commonlib.config.RestClientProperties;
import com.example.commonlib.exception.BadRequestException;
import com.example.commonlib.exception.InternalServerErrorException;
import com.example.commonlib.exception.NotFoundException;
import com.example.commonlib.exception.RemoteServiceException;
import com.example.commonlib.model.RemoteErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public class CommonRestClient {

    private static final Logger log = LoggerFactory.getLogger(CommonRestClient.class);

    private final RestClient restClient;
    private final RetryExecutor retryExecutor;
    private final CircuitBreaker circuitBreaker;

    public CommonRestClient(RestClientProperties props) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.getConnectionTimeout());
        factory.setReadTimeout(props.getReadTimeout());
        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .build();

        this.retryExecutor = new RetryExecutor(props.getRetryProperties());
        this.circuitBreaker = new CircuitBreaker(props.getCircuitBreakerProperties());
    }

    public <T> T get(String url, Class<T> responseType) throws Exception {
        if (!circuitBreaker.allowRequest()) {
            log.warn("Circuit breaker is open - Skipping call");
            throw new RemoteServiceException(
                    new RemoteErrorResponse(
                            503,
                            "Service unavailable",
                            "Circuit breaker is open - Skipping call",
                            url
                    )
            );
        }

        try {
            return retryExecutor.executeWithRetry(() -> {
                try {
                    log.debug("Calling URL: {}", url);
                    T response = restClient.get()
                            .uri(url)
                            .retrieve()
                            .body(responseType);
                    circuitBreaker.recordSuccess();
                    log.debug("Successful response from URL: {}", url);
                    return response;
                } catch (HttpStatusCodeException ex) {
                    circuitBreaker.recordFailure();
                    log.error("Failed to call URL: {}", url);
                    throw mapException(url, ex);
                } catch (RestClientException ex) {
                    circuitBreaker.recordFailure();
                    log.error("Failed to call URL: {}", url);
                    throw new RemoteServiceException(
                            new RemoteErrorResponse(
                                    500,
                                    "Remote Service Error",
                                    ex.getMessage(),
                                    url
                            )
                    );
                }
            });
        } catch (Exception ex) {
            log.error("Unexpected Error from URL: {}", url);
            if (ex instanceof RemoteServiceException remoteServiceException) throw remoteServiceException;
            throw new RemoteServiceException(
                    new RemoteErrorResponse(
                            500,
                            "Unexpected Error",
                            ex.getMessage(),
                            url
                    )
            );
        }
    }

    private Exception mapException(String url, HttpStatusCodeException ex) {
        HttpStatus statusCode = (HttpStatus) ex.getStatusCode();

        RemoteErrorResponse errorResponse = new RemoteErrorResponse(
                statusCode.value(),
                statusCode.getReasonPhrase(),
                ex.getResponseBodyAsString(),
                url
        );

        switch (statusCode) {
            case BAD_REQUEST -> throw new BadRequestException(errorResponse);
            case NOT_FOUND -> throw new NotFoundException(errorResponse);
            case INTERNAL_SERVER_ERROR -> throw new InternalServerErrorException(errorResponse);
            default -> throw new RemoteServiceException(errorResponse);
        }
    }
}
