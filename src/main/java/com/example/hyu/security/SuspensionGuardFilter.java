package com.example.hyu.security;

import com.example.hyu.entity.Users;
import com.example.hyu.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;

@RequiredArgsConstructor
public class SuspensionGuardFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthPrincipal principal) {
            userRepository.findById(principal.id()).ifPresent(u -> {
                Instant now = Instant.now();

                boolean withdrawn = u.getState() == Users.UserState.WITHDRAWN;
                boolean suspended = u.getState() == Users.UserState.SUSPENDED &&
                        (u.getSuspendUntil() == null || u.getSuspendUntil().isAfter(now));

                if (withdrawn || suspended) {
                    SecurityContextHolder.clearContext();
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json;charset=UTF-8");
                    String code = withdrawn ? "ACCOUNT_WITHDRAWN" : "ACCOUNT_SUSPENDED";
                    try {
                        response.getWriter().write("{\"error\":\"" + code + "\"}");
                    } catch (IOException ignored) {}
                }
            });
            if (response.isCommitted()) return; // 위에서 이미 응답했으면 중단
        }

        chain.doFilter(request, response);
    }
}