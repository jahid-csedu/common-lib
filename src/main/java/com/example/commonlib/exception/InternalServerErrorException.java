package com.example.commonlib.exception;

import com.example.commonlib.model.RemoteErrorResponse;

public class InternalServerErrorException extends RemoteServiceException{
    public InternalServerErrorException(RemoteErrorResponse errorResponse) {
        super(errorResponse);
    }
}
