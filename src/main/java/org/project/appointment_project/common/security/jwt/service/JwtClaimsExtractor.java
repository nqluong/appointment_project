package org.project.appointment_project.common.security.jwt.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.common.security.jwt.principal.JwtUserPrincipal;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

//Trich xuat claim tu jwt token
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class JwtClaimsExtractor {
    static String USER_ID_CLAIM = "userId";
    static String USERNAME_CLAIM = "username";
    static String ROLES_CLAIM = "roles";
    static String EMAIL_CLAIM = "email";

    public JwtUserPrincipal extractPrincipal(Jwt jwt) {
        UUID userId = extractUserId(jwt);
        String username = extractUsername(jwt);
        String email = extractEmail(jwt);
        List<String> roles = extractRoles(jwt);

        return JwtUserPrincipal.builder()
                .userId(userId)
                .username(username)
                .email(email)
                .roles(roles)
                .build();
    }

    public Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        List<String> roles = extractRoles(jwt);
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                .collect(Collectors.toList());
    }

    private UUID extractUserId(Jwt jwt) {
        String userIdStr = jwt.getClaimAsString(USER_ID_CLAIM);
        try {
            return UUID.fromString(userIdStr);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid user_id in token");
        }
    }

    private String extractUsername(Jwt jwt) {
        return jwt.getSubject();
    }

    private String extractEmail(Jwt jwt) {
        return jwt.getClaimAsString(EMAIL_CLAIM);
    }

    private List<String> extractRoles(Jwt jwt) {
        try {
            return jwt.getClaimAsStringList(ROLES_CLAIM);
        } catch (Exception e) {
            log.warn("Invalid roles format in JWT: {}", e.getMessage());
            return List.of();
        }
    }
}
