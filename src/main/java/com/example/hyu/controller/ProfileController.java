package com.example.hyu.controller;

import com.example.hyu.dto.user.ProfileResponse;
import com.example.hyu.dto.user.ProfileUpdateRequest;
import com.example.hyu.security.CurrentUser;
import com.example.hyu.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.example.hyu.security.AuthPrincipal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/profiles")
public class ProfileController {

    private final ProfileService profileService;

    /**
     * Returns the current authenticated user's profile.
     *
     * @param me the authenticated principal used to resolve the current user's ID
     * @return a 200 OK ResponseEntity containing the current user's ProfileResponse
     */
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<ProfileResponse> me(@AuthenticationPrincipal AuthPrincipal me) {
        return ResponseEntity.ok(profileService.getMyProfile(me.getUserId()));
    }

    /**
     * Creates or updates the authenticated user's profile and returns the saved profile.
     *
     * Delegates to ProfileService.upsertMyProfile using the current user's ID and the validated
     * update payload. Responds with HTTP 200 and the resulting ProfileResponse.
     *
     * @param me  the currently authenticated principal (used to obtain the user's ID)
     * @param req the validated profile update payload
     * @return a ResponseEntity containing the saved ProfileResponse with HTTP 200 OK
     */
    @PutMapping("/me")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<ProfileResponse> upsert(
            @AuthenticationPrincipal AuthPrincipal me,
            @RequestBody @Valid ProfileUpdateRequest req
    ) {
        return ResponseEntity.ok(profileService.upsertMyProfile(me.getUserId(), req));
    }
}
