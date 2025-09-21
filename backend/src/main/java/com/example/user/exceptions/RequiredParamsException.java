package com.example.user.exceptions;

import org.springframework.http.HttpStatus;

public class RequiredParamsException extends UserException {
    public RequiredParamsException(String message) {
        super(HttpStatus.BAD_REQUEST.value(), message);
    }
}
