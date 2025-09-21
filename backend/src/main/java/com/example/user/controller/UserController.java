package com.example.user.controller;

import com.example.user.api.*;
import com.example.user.service.UserService;
import com.example.user.utils.ResponseBuilder;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.UUID;

@RestController
@RequestMapping(path = "/api/users", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserController {
    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponseDto<UserResponse>> create(@Valid @RequestBody CreateUserRequest req) {
        return ResponseBuilder.created(userService.create(req));
    }

    @GetMapping
    public ResponseEntity<ApiResponseDto<PagedResponse<UserResponse>>> userList(@RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) String q,
                                                       @RequestParam(defaultValue = "true") boolean activeOnly, @RequestParam(required = false) String sort) {
        return ResponseBuilder.success(PagedResponse.toPagedResponse(userService.getList(q, activeOnly, page, size, sort)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDto<UUID>> delete(@PathVariable UUID id,
                                                       @RequestParam(defaultValue = "true") boolean soft) {
        return ResponseBuilder.success(userService.delete(id, soft));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponseDto<UserStatsResponseDto>> stats() {
        return ResponseBuilder.success(userService.getStats());
    }




}

