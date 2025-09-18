package org.example.iw_order_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AuthFilter extends OncePerRequestFilter {

    private final AuthInfo currentAuth;

    public AuthFilter(AuthInfo currentAuth) {
        this.currentAuth = currentAuth;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String userIdHeader = request.getHeader("X-User-Id");
        String rolesHeader = request.getHeader("X-Roles");

        if (userIdHeader != null && !userIdHeader.isBlank()) {
            try {
                Long userId = Long.parseLong(userIdHeader);
                currentAuth.setUserId(userId);
                currentAuth.setRoles(rolesHeader != null ? rolesHeader : "");
            } catch (NumberFormatException e) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid X-User-Id");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}

