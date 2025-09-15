package com.example.hyu.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.util.Map;

public class CustomAccessDeniedHandler implements AccessDeniedHandler {
    private final ObjectMapper om = new ObjectMapper();

    /**
     * Handles an AccessDeniedException by returning a 403 Forbidden response with a JSON payload.
     *
     * The response Content-Type is set to "application/json" and the body contains:
     * {"error":"FORBIDDEN","message":"접근 권한이 없습니다."}
     *
     * @param request the HTTP request that resulted in access denial
     * @param response the HTTP response to write the 403 JSON payload to
     * @param accessDeniedException the exception that triggered this handler
     * @throws IOException if an I/O error occurs while writing the response
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        om.writeValue(response.getOutputStream(),
                Map.of("error", "FORBIDDEN", "message", "접근 권한이 없습니다."));
    }
}
