package com.example.hyu.controller;

import com.example.hyu.dto.user.UserAuthResponse;
import com.example.hyu.dto.user.UserLoginRequest;
import com.example.hyu.dto.user.UserResponse;
import com.example.hyu.dto.user.UserSignupRequest;
import com.example.hyu.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
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
     * Registers a new user with the provided signup data.
     *
     * Delegates user creation to AuthService and returns the created user's
     * UserResponse wrapped in a 200 OK ResponseEntity.
     *
     * @param req validated signup payload (e.g., username, password, email)
     * @return ResponseEntity containing the created UserResponse with HTTP 200 OK
     */
    @PostMapping("/signup")
    public ResponseEntity<UserResponse> signup(@RequestBody @Valid UserSignupRequest req) {
        UserResponse created = authService.signup(req);
        return ResponseEntity.ok(created);
    }

    /**
     * Authenticates a user with credentials and returns authentication information.
     *
     * Accepts a validated UserLoginRequest containing user credentials and returns
     * a UserAuthResponse (e.g., tokens and user metadata) on successful authentication.
     *
     * @param req validated login request containing the user's credentials
     * @return ResponseEntity with a UserAuthResponse and HTTP 200 OK on success
     */
    @PostMapping("/login")
    public ResponseEntity<UserAuthResponse> login(@RequestBody @Valid UserLoginRequest req,
                                                  HttpServletRequest httpReq) {
        UserAuthResponse auth = authService.login(req, httpReq);
        return ResponseEntity.ok(auth);
    }
}
