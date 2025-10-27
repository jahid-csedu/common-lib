package com.example.commonlib.client;

import com.example.commonlib.config.RestClientProperties;
import com.example.commonlib.exception.BadRequestException;
import com.example.commonlib.exception.InternalServerErrorException;
import com.example.commonlib.exception.NotFoundException;
import com.example.commonlib.exception.RemoteServiceException;
import com.example.commonlib.model.RemoteErrorResponse;
import com.example.commonlib.tracing.RequestSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.util.concurrent.Callable;

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

        this.retryExecutor = props.getRetry() != null
                ? new RetryExecutor(props.getRetry())
                : null;

        this.circuitBreaker = props.getCircuitBreaker() != null
                ? new CircuitBreaker(props.getCircuitBreaker())
                : null;
    }

    /**
     * Executes an HTTP GET request to the specified URL and maps the response body to the given type.
     * <p>
     * This method integrates optional retry and circuit breaker mechanisms:
     * <ul>
     *     <li><strong>Retry:</strong> If {@code RetryProperties} are configured, the request will be retried
     *     according to the defined retry policy (e.g., max attempts, delay, backoff multiplier).</li>
     *     <li><strong>Circuit Breaker:</strong> If {@code CircuitBreakerProperties} are configured, the circuit breaker
     *     monitors failures. When failures exceed the defined threshold, further requests are blocked until the
     *     circuit transitions back to the half-open state after the configured duration.</li>
     * </ul>
     * <p>
     * The method automatically handles and maps HTTP errors to domain-specific exceptions such as:
     * {@link com.example.commonlib.exception.BadRequestException},
     * {@link com.example.commonlib.exception.NotFoundException},
     * {@link com.example.commonlib.exception.InternalServerErrorException},
     * and {@link com.example.commonlib.exception.RemoteServiceException}.
     *
     * @param url          the URL to call
     * @param responseType the type of the expected response body
     * @param <T>          the response type
     * @return the response body mapped to {@code responseType}
     * @throws com.example.commonlib.exception.RemoteServiceException       if the remote call fails or the circuit breaker is open
     * @throws com.example.commonlib.exception.BadRequestException          if the server returns HTTP 400 (Bad Request)
     * @throws com.example.commonlib.exception.NotFoundException            if the server returns HTTP 404 (Not Found)
     * @throws com.example.commonlib.exception.InternalServerErrorException if the server returns HTTP 500 (Internal Server Error)
     */
    public <T> T get(String url, Class<T> responseType) {
        RequestSpan span = RequestSpan.start();
        span.logStart(log, url);
        checkIfCircuitBreakerClosed(url);

        Callable<T> callable = () -> {
            int attempt = retryExecutor != null ? retryExecutor.getCurrentAttempt() : 1;
            span.logRetry(log, attempt, url);
            T response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(responseType);

            recordCircuitBreakerSuccess();

            span.logSuccess(log, url);
            return response;
        };

        return doCall(url, callable, span);
    }

    /**
     * Executes an HTTP POST request to the specified URL with the given request body and maps
     * the response body to the specified response type.
     *
     * Supports optional retry and circuit breaker mechanisms based on configuration.
     *
     * @param url           the target URL
     * @param requestBody   the body of the POST request (may be null)
     * @param responseType  the type of the expected response
     * @param <T>           the request body type
     * @param <R>           the response body type
     * @return the response body mapped to {@code responseType}
     * @throws RemoteServiceException, BadRequestException, NotFoundException, InternalServerErrorException
     *         for various HTTP and connection errors
     */
    public <T, R> R post(String url, T requestBody, Class<R> responseType) {
        RequestSpan span = RequestSpan.start();
        span.logStart(log, url);
        checkIfCircuitBreakerClosed(url);

        Callable<R> callable = () -> {
            int attempt = retryExecutor != null ? retryExecutor.getCurrentAttempt() : 1;
            span.logRetry(log, attempt, url);
            R response = restClient.post()
                    .uri(url)
                    .body(requestBody)
                    .retrieve()
                    .body(responseType);

            recordCircuitBreakerSuccess();
            span.logSuccess(log, url);

            return response;
        };

        return doCall(url, callable, span);
    }

    private <T> T doCall(String url, Callable<T> callable, RequestSpan span) {
        try {
            if (retryExecutor != null) {
                return retryExecutor.executeWithRetry(callable);
            } else {
                return callable.call();
            }
        } catch (Exception ex) {
            recordCircuitBreakerFailure();
            span.logFailure(log, url, ex);

            if (ex instanceof HttpStatusCodeException statusEx) {
                throw mapException(url, statusEx);
            }

            if (ex instanceof RemoteServiceException remoteServiceException) {
                throw remoteServiceException;
            }

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

    private void recordCircuitBreakerSuccess() {
        if (circuitBreaker != null) {
            circuitBreaker.recordSuccess();
        }
    }

    private void recordCircuitBreakerFailure() {
        if (circuitBreaker != null) {
            circuitBreaker.recordFailure();
        }
    }

    private void checkIfCircuitBreakerClosed(String url) {
        if (circuitBreaker != null && !circuitBreaker.allowRequest()) {
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
    }

    private RuntimeException mapException(String url, HttpStatusCodeException ex) {
        HttpStatusCode statusCode = ex.getStatusCode();

        RemoteErrorResponse errorResponse = new RemoteErrorResponse(
                statusCode.value(),
                statusCode.toString(),
                ex.getResponseBodyAsString(),
                url
        );

        HttpStatus resolved = HttpStatus.resolve(statusCode.value());
        if (resolved != null) {
            switch (resolved) {
                case BAD_REQUEST -> throw new BadRequestException(errorResponse);
                case NOT_FOUND -> throw new NotFoundException(errorResponse);
                case INTERNAL_SERVER_ERROR -> throw new InternalServerErrorException(errorResponse);
                default -> throw new RemoteServiceException(errorResponse);
            }
        }
        throw new RemoteServiceException(errorResponse);
    }
}
