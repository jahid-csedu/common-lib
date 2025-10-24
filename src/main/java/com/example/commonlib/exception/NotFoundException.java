package com.example.commonlib.exception;

import com.example.commonlib.model.RemoteErrorResponse;

public class NotFoundException extends RemoteServiceException {
    public NotFoundException(RemoteErrorResponse errorResponse) {
        super(errorResponse);
    }
}
