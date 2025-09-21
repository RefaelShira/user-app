package com.example.user.exceptions;

import org.springframework.http.HttpStatus;

public class DuplicateRecordException extends UserException {
    public DuplicateRecordException(String message) {
        super(HttpStatus.CONFLICT.value(), message);
    }
}
