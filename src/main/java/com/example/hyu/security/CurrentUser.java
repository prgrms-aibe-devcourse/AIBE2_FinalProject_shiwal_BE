package com.example.hyu.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {
    /**
 * Prevents instantiation of this utility class.
 */
private CurrentUser(){}

    /**
     * Returns the authenticated user's ID, if available.
     *
     * <p>Retrieves the current Authentication from Spring Security and, if the principal is an
     * AuthPrincipal, returns its userId. Returns {@code null} when there is no authentication or
     * when the principal is not an AuthPrincipal.
     *
     * @return the current user's ID, or {@code null} if unavailable
     */
    public static Long id() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null) return null;
        Object p = a.getPrincipal();
        if (p instanceof AuthPrincipal ap) return ap.getUserId();
        return null;
    }

    /**
     * Returns the current authenticated AuthPrincipal from the Spring Security context.
     *
     * @return the current AuthPrincipal, or null if there is no authentication or the principal is not an AuthPrincipal
     */
    public static AuthPrincipal principal() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null) return null;
        Object p = a.getPrincipal();
        return (p instanceof AuthPrincipal ap) ? ap : null;
    }
}
    