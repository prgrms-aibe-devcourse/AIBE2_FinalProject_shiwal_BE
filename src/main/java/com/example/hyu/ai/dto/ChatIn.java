package com.example.hyu.ai.dto;

public record ChatIn(
        String session_id,
        String message,
        Object context,
        Object user_id) {
}