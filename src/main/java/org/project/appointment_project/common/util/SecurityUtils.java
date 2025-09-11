package org.project.appointment_project.common.util;

import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.common.exception.CustomException;
import org.project.appointment_project.common.exception.ErrorCode;
import org.project.appointment_project.common.security.jwt.converter.CustomJwtAuthenticationToken;
import org.project.appointment_project.common.security.jwt.principal.JwtUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class SecurityUtils {

    public void validateUserAccess(UUID targetUserId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Unauthenticated access attempt to user: {}", targetUserId);
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        UUID currentUserId = getCurrentUserId();
        log.debug("Validating access: currentUserId={}, targetUserId={}", currentUserId, targetUserId);

        if (hasAdminRole(authentication)) {
            log.info("Admin access granted for user: {} by admin: {}", targetUserId, currentUserId);
            return;
        }

        if (currentUserId.equals(targetUserId)) {
            log.info("Self access granted for user: {}", targetUserId);
            return;
        }

        log.warn("Access denied: User {} attempted to access user {}", currentUserId, targetUserId);
        throw new CustomException(ErrorCode.FORBIDDEN);
    }

    public UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("No authenticated user found");
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        try {
            // Handle CustomJwtAuthenticationToken
            if (authentication instanceof CustomJwtAuthenticationToken customToken) {
                JwtUserPrincipal principal = customToken.getUserPrincipal();
                log.debug("Extracted userId from CustomJwtAuthenticationToken: {}", principal.getUserId());
                return principal.getUserId();
            }

            // Handle standard JwtAuthenticationToken (fallback)
            if (authentication instanceof JwtAuthenticationToken jwtToken) {
                Jwt jwt = jwtToken.getToken();
                String userIdStr = jwt.getClaimAsString("userId");
                if (userIdStr != null) {
                    UUID userId = UUID.fromString(userIdStr);
                    log.debug("Extracted userId from JWT token claims: {}", userId);
                    return userId;
                }
            }

            // Final fallback - try to parse from authentication name
            String userIdStr = authentication.getName();
            if (userIdStr != null) {
                try {
                    UUID userId = UUID.fromString(userIdStr);
                    log.debug("Extracted userId from authentication name: {}", userId);
                    return userId;
                } catch (IllegalArgumentException e) {
                    log.warn("Authentication name is not a valid UUID: {}", userIdStr);
                }
            }

            log.error("Could not extract userId from authentication: {}", authentication.getClass().getSimpleName());
            throw new CustomException(ErrorCode.TOKEN_INVALID);

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error extracting userId from authentication", e);
            throw new CustomException(ErrorCode.TOKEN_INVALID);
        }
    }

    private boolean hasAdminRole(Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities()
                .stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        log.debug("User has admin role: {}", isAdmin);
        return isAdmin;
    }

    public boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            log.debug("No authentication found when checking role: {}", role);
            return false;
        }

        String roleWithPrefix = "ROLE_" + role.toUpperCase();
        boolean hasRole = authentication.getAuthorities()
                .stream()
                .anyMatch(authority -> authority.getAuthority().equals(roleWithPrefix));

        log.debug("User has role '{}': {}", role, hasRole);
        return hasRole;
    }

    public UUID getCurrentUserIdSafe() {
        try {
            return getCurrentUserId();
        } catch (Exception e) {
            log.debug("Could not get current user ID safely", e);
            return null;
        }
    }

    public boolean isCurrentUserAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && hasAdminRole(authentication);
    }

    public JwtUserPrincipal getCurrentUserPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof CustomJwtAuthenticationToken customToken) {
            return customToken.getUserPrincipal();
        }

        log.error("Cannot get JwtUserPrincipal from authentication type: {}",
                authentication != null ? authentication.getClass().getSimpleName() : "null");
        throw new CustomException(ErrorCode.UNAUTHORIZED);
    }
}
