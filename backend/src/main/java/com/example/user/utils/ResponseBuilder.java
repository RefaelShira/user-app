package com.example.user.utils;
import com.example.user.api.ApiResponseDto;
import org.springframework.http.ResponseEntity;

public class ResponseBuilder {
    public static <T> ResponseEntity<ApiResponseDto<T>> success(T data) {
        return ResponseEntity.ok(
                ApiResponseDto.<T>builder()
                        .code(200)
                        .status("OK")
                        .data(data)
                        .build()
        );
    }

    public static <T> ResponseEntity<ApiResponseDto<T>> created(T data) {
        return ResponseEntity.status(201).body(
                ApiResponseDto.<T>builder()
                        .code(201)
                        .status("CREATED")
                        .data(data)
                        .build()
        );
    }

    public static <T> ResponseEntity<ApiResponseDto<T>> error(int code, String status, String errorMessage) {
        return ResponseEntity.status(code).body(
                ApiResponseDto.<T>builder()
                        .code(code)
                        .status(status)
                        .error(errorMessage)
                        .build()
        );
    }
}
