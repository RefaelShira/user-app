package com.example.user.exceptions;

import org.springframework.http.HttpStatus;

public class ShortPasswordException extends UserException {
    public ShortPasswordException(String message) {
        super(HttpStatus.BAD_REQUEST.value(), message);
    }
}
