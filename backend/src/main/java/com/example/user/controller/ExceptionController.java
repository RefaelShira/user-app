package com.example.user.controller;

import com.example.user.exceptions.*;
import com.example.user.api.ApiResponseDto;
import com.example.user.utils.ResponseBuilder;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.NoSuchElementException;

@Slf4j
@ControllerAdvice
public class ExceptionController {

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleNoSuchElement(NoSuchElementException ex) {
        return ResponseBuilder.error(HttpStatus.NOT_FOUND.value(), "NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(NotFoundRecordException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleNotFound(NotFoundRecordException ex) {
        return ResponseBuilder.error(HttpStatus.NOT_FOUND.value(), "NOT_FOUND", ex.getMessage());
    }
    @ExceptionHandler(DuplicateRecordException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleDuplicateRecord(DuplicateRecordException ex) {
        return ResponseBuilder.error(HttpStatus.CONFLICT.value(), "CONFLICT", ex.getMessage());
    }

    @ExceptionHandler(UserAlreadyInactiveException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleAlreadyInactive(UserAlreadyInactiveException ex) {
        return ResponseBuilder.error(HttpStatus.UNPROCESSABLE_ENTITY.value(), "ALREADY_INACTIVE", ex.getMessage());
    }

    @ExceptionHandler(WeakPasswordException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleWeakPassword(WeakPasswordException ex) {
        return ResponseBuilder.error(HttpStatus.BAD_REQUEST.value(), "BAD_REQUEST", ex.getMessage());
    }
    @ExceptionHandler(ShortPasswordException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleShortPassword(ShortPasswordException ex) {
        return ResponseBuilder.error(HttpStatus.BAD_REQUEST.value(), "BAD_REQUEST", ex.getMessage());
    }
    @ExceptionHandler(RequiredParamsException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleRequiredParams(RequiredParamsException ex) {
        return ResponseBuilder.error(HttpStatus.BAD_REQUEST.value(), "BAD_REQUEST", ex.getMessage());
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleValidation(MethodArgumentNotValidException ex) {
        return ResponseBuilder.error(400, "VALIDATION_ERROR", "Validation failed");
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleGeneric(HttpMessageNotReadableException ex) {
        return ResponseBuilder.error(HttpStatus.BAD_REQUEST.value(), "MALFORMED_JSON", "Request body is not readable JSON");
    }
    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<ApiResponseDto<Void>> handleServiceUnavailable(CallNotPermittedException ex) {
        return ResponseBuilder.error(HttpStatus.SERVICE_UNAVAILABLE.value(), "SERVICE_UNAVAILABLE", "Please try again later");
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDto<Void>> handleGeneric(Exception ex) {
        return ResponseBuilder.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "INTERNAL_ERROR", "Unexpected error");
    }

}
