package com.example.user.exceptions;

import org.springframework.http.HttpStatus;

public class UserException extends RuntimeException{
    Integer status;
    public UserException(String message, Throwable cause) {
        this(HttpStatus.INTERNAL_SERVER_ERROR.value(), message, cause);
    }

    public UserException(String message) {
        this(HttpStatus.INTERNAL_SERVER_ERROR.value(), message);
    }

    public UserException(int status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public UserException(int status, String message) {
        super(message);
        this.status = status;
    }

    public Integer getStatus() {
        return status;
    }
}
