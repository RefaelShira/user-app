package com.example.user.exceptions;

public class UserAlreadyInactiveException extends UserException {
    public UserAlreadyInactiveException(String message) {
        super(message);
    }
}
