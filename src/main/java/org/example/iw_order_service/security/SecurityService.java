package org.example.iw_order_service.security;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.iw_order_service.exception.ForbiddenException;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@Slf4j
@RequiredArgsConstructor
public class SecurityService {
    private final AuthInfo authInfo;

    public boolean hasRole(String role) {
        if (authInfo.getRoles() == null || authInfo.getRoles().isEmpty()) {
            return false;
        }
        return Arrays.asList(authInfo.getRoles().split(","))
                .stream()
                .anyMatch(r -> r.trim().equals(role));
    }
    public Long getCurrentUserId() {
        if (authInfo.getUserId() == null) {
            throw new ForbiddenException("Missing authentication headers");
        }
        return authInfo.getUserId();
    }
}
