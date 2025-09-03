package com.example.hyu.dto.user;

public record UserAuthResponse(
        String accessToken,
        String tokenType,   // "Bearer"
        long expiresIn      // 초 단위 (선택)
) {
    public static UserAuthResponse bearer(String token, long expiresIn) {
        return new UserAuthResponse(token, "Bearer", expiresIn);
    }
}
