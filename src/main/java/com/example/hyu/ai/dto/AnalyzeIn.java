package com.example.hyu.ai.dto;

public record AnalyzeIn(
        String text,
        Object mood_slider,
        Object tags) {
}