package com.example.hyu.controller;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles validation failures from @Valid method arguments and returns a 400 Bad Request.
     *
     * Extracts the first field error (if any) and formats it as "field: message"; if no field
     * error is available returns "Validation error". The response body is a JSON object
     * containing an "error" property with that message.
     *
     * @param e the MethodArgumentNotValidException thrown for invalid method arguments
     * @return a ResponseEntity with HTTP 400 and a body of the form {@code {"error": "<message>"}} 
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .orElse("Validation error");
        return ResponseEntity.badRequest().body(Map.of("error", message));
    }

    /**
     * Handles a jakarta.validation.ConstraintViolationException and converts it into an HTTP 400 Bad Request response.
     *
     * <p>The response body is a map with a single `"error"` entry containing the exception's message.</p>
     *
     * @return ResponseEntity with status 400 and body Map.of("error", exceptionMessage)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<?> handleConstraint(ConstraintViolationException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    /**
     * Handles IllegalArgumentException by returning an HTTP 401 response containing the exception message.
     *
     * The response body is a JSON object with a single "error" key whose value is the exception's message.
     *
     * @param e the IllegalArgumentException that was thrown
     * @return a ResponseEntity with status 401 (UNAUTHORIZED) and body {@code {"error": "<message>"}} 
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArg(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
    }

    /**
     * Handles any uncaught exception and returns a generic 500 Internal Server Error response.
     *
     * This prevents exception details from being exposed to clients by returning a fixed
     * JSON body with an "error" key set to "Internal server error".
     *
     * @param e the caught exception
     * @return a ResponseEntity with HTTP 500 and body {@code {"error":"Internal server error"}}
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleEtc(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Internal server error"));
    }
}
