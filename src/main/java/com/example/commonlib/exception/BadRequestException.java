package com.example.commonlib.exception;

import com.example.commonlib.model.RemoteErrorResponse;

public class BadRequestException extends RemoteServiceException {
    public BadRequestException(RemoteErrorResponse errorResponse) {
        super(errorResponse);
    }
}
