package com.example.hyu.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.util.Map;

public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper om = new ObjectMapper();

    /**
     * Sends a 401 Unauthorized response with a JSON error payload.
     *
     * Sets the response status to 401, Content-Type to "application/json", and writes the
     * JSON object: {"error":"UNAUTHORIZED","message":"유효한 인증이 필요합니다."}.
     *
     * @param request the incoming HTTP request
     * @param response the HTTP response that will be modified and written to
     * @param authException the authentication exception that triggered this entry point
     * @throws IOException if writing the JSON to the response output stream fails
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        om.writeValue(response.getOutputStream(),
                Map.of("error", "UNAUTHORIZED", "message", "유효한 인증이 필요합니다."));
    }
}
