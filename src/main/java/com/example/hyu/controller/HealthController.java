package com.example.hyu.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    /**
     * Health check endpoint that returns "OK".
     *
     * <p>Mapped to GET /health; the returned string is written to the HTTP response body
     * and can be used by monitoring systems to verify the application is running.
     *
     * @return the literal string "OK" indicating the application is healthy
     */
    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
