package com.example.hyu.core.common;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> badRequest() {
        return ResponseEntity.badRequest().body("요청 값이 올바르지 않습니다.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> conflict(IllegalArgumentException e) {
        return ResponseEntity.status(409).body(e.getMessage());
    }
}