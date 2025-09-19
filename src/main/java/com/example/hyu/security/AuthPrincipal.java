package com.example.hyu.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthPrincipal {
    private final Long userId;
    private final String email;   // nullable
    private final String role;    // e.g. "ROLE_USER"
}