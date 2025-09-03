package com.example.hyu.dto.user;

public record UserAuthResponse(
        String accessToken,
        String tokenType,   // "Bearer"
        long expiresIn      // 초 단위 (선택)
) {
    /**
     * Creates a UserAuthResponse for a Bearer token.
     *
     * @param token     the access token string
     * @param expiresIn the token lifetime in seconds (may be zero if unspecified)
     * @return a UserAuthResponse with {@code tokenType} set to {@code "Bearer"}
     */
    public static UserAuthResponse bearer(String token, long expiresIn) {
        return new UserAuthResponse(token, "Bearer", expiresIn);
    }
}
