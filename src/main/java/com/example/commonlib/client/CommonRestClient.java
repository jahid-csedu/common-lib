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

import java.util.Objects;
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

    public <T> T get(String url, Class<T> responseType) throws Exception {
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

        Callable<T> call = () -> {
            log.debug("Calling URL: {}", url);
            T response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(responseType);

            if (circuitBreaker != null) {
                circuitBreaker.recordSuccess();
            }

            log.debug("Successful response from URL: {}", url);
            return response;
        };

        try {
            if (Objects.nonNull(retryExecutor)) {
                return retryExecutor.executeWithRetry(call);
            } else {
                return call.call();
            }
        } catch (Exception ex) {
            if (circuitBreaker != null) {
                circuitBreaker.recordFailure();
            }
            log.error("Error during call to URL: {}", url, ex);

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
