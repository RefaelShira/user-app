package com.example.user.exceptions;

import org.springframework.http.HttpStatus;

public class WeakPasswordException extends UserException {
    public WeakPasswordException(String message) {
        super(HttpStatus.BAD_REQUEST.value(), message);
    }
}
