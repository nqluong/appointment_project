package org.project.appointment_project.user.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.common.security.jwt.principal.JwtUserPrincipal;
import org.project.appointment_project.common.util.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Slf4j
public class TestController {

    private final SecurityUtils securityUtils;

    @GetMapping("/auth-info")
    public ResponseEntity<Map<String, Object>> getAuthInfo() {
        Map<String, Object> authInfo = new HashMap<>();

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            authInfo.put("authenticationType", auth != null ? auth.getClass().getSimpleName() : "null");
            authInfo.put("isAuthenticated", auth != null && auth.isAuthenticated());
            authInfo.put("authorities", auth != null ? auth.getAuthorities() : "null");
            authInfo.put("name", auth != null ? auth.getName() : "null");
            authInfo.put("principalType", auth != null && auth.getPrincipal() != null ?
                    auth.getPrincipal().getClass().getSimpleName() : "null");

            // Try to get current user ID
            try {
                UUID currentUserId = securityUtils.getCurrentUserId();
                authInfo.put("currentUserId", currentUserId);
            } catch (Exception e) {
                authInfo.put("currentUserIdError", e.getMessage());
            }


            try {
                JwtUserPrincipal principal = securityUtils.getCurrentUserPrincipal();
                authInfo.put("userPrincipal", Map.of(
                        "userId", principal.getUserId(),
                        "username", principal.getUsername(),
                        "email", principal.getEmail(),
                        "roles", principal.getRoles()
                ));
            } catch (Exception e) {
                authInfo.put("userPrincipalError", e.getMessage());
            }

        } catch (Exception e) {
            authInfo.put("error", e.getMessage());
        }

        return ResponseEntity.ok(authInfo);
    }
}
