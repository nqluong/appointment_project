package org.project.appointment_project.common.security.jwt.service;

import org.project.appointment_project.auth.dto.TokenPair;
import org.project.appointment_project.user.enums.RoleName;

import java.util.List;
import java.util.UUID;

public interface TokenService {
    TokenPair generateTokens(UUID userId, String username, List<RoleName> roles);
    boolean validateToken(String token);
    UUID getUserIdFromToken(String token);
    String getEmailFromToken(String token);
    List<String> getRolesFromToken(String token);
    String getTokenType(String token);
    boolean isTokenExpired(String token);
    String hashToken(String token);
}
