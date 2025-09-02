package com.example.hyu.core.auth.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
    public static LoginResponse of(String token, long expiresIn) {
        return new LoginResponse(token, "Bearer", expiresIn);
    }
}
