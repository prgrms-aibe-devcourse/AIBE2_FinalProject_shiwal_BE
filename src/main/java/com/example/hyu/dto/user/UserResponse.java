package com.example.hyu.dto.user;

public record UserResponse(
        Long id,
        String email,
        String name,
        String nickname,
        String role
) {}
