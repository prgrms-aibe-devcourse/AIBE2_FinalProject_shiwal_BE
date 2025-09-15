package com.example.hyu.controller;

import com.example.hyu.dto.user.UserAuthResponse;
import com.example.hyu.dto.user.UserLoginRequest;
import com.example.hyu.dto.user.UserResponse;
import com.example.hyu.dto.user.UserSignupRequest;
import com.example.hyu.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    /**
     * Registers a new user from the JSON signup payload.
     *
     * <p>Accepts a validated UserSignupRequest from the request body, delegates creation to the service,
     * and returns the created user's public representation.</p>
     *
     * @param req the validated signup payload (request body)
     * @return a ResponseEntity with a UserResponse containing the created user's public data (HTTP 200)
     */
    @PostMapping("/signup")
    public ResponseEntity<UserResponse> signup(@RequestBody @Valid UserSignupRequest req) {
        return ResponseEntity.ok(authService.signup(req));
    }

    /**
     * Authenticate a user and return authentication details.
     *
     * Delegates authentication to AuthService; on success it writes the refresh-token (RT) cookie to
     * the provided HttpServletResponse and returns a 200 OK with a UserAuthResponse (typically containing
     * an access token and user info).
     *
     * @param req validated login credentials
     * @return 200 OK with the authenticated user's auth payload
     */
    @PostMapping("/login")
    public ResponseEntity<UserAuthResponse> login(@RequestBody @Valid UserLoginRequest req,
                                                  HttpServletResponse res,
                                                  HttpServletRequest httpReq) {
        return ResponseEntity.ok(authService.login(req, httpReq, res));
    }

    /**
     * Refreshes authentication tokens by delegating to the AuthService.
     *
     * <p>Reads the incoming refresh token from the request (e.g. cookie or header) and issues rotated
     * tokens, writing any response-side artifacts (e.g. updated cookies) to the HttpServletResponse.
     *
     * @return 200 OK with a UserAuthResponse containing the new access token and related auth data
     */
    @PostMapping("/refresh")
    public ResponseEntity<UserAuthResponse> refresh(HttpServletRequest req, HttpServletResponse res) {
        return ResponseEntity.ok(authService.refresh(req, res));
    }

    /**
     * Logs the user out by invalidating refresh token(s) and clearing authentication cookies; responds with HTTP 204 No Content.
     *
     * This operation is idempotent — it returns 204 even if no refresh token is present.
     *
     * @param allDevices when true, revoke sessions and refresh tokens across all devices; when false, revoke only the current session
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestParam(defaultValue = "false") boolean allDevices,
                                       HttpServletRequest req, HttpServletResponse res) {
        authService.logout(req, res, allDevices);
        return ResponseEntity.noContent().build();
    }
}
