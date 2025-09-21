package com.example.user.exceptions;

import org.springframework.http.HttpStatus;

public class NotFoundRecordException extends UserException {
    public NotFoundRecordException(String message) {
        super(HttpStatus.NOT_FOUND.value(), message);
    }
}
