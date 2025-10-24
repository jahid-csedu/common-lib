package com.example.commonlib.exception;

import com.example.commonlib.model.RemoteErrorResponse;

public class RemoteServiceException extends RuntimeException {
    private final RemoteErrorResponse errorResponse;

    public RemoteServiceException(RemoteErrorResponse errorResponse) {
        super(errorResponse.getMessage());
        this.errorResponse = errorResponse;
    }

    public RemoteErrorResponse getErrorResponse() {
        return this.errorResponse;
    }
}
