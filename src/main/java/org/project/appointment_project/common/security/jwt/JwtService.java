package org.project.appointment_project.common.security.jwt;

import java.util.List;
import java.util.UUID;

public interface JwtService {
    String generateAccessToken(UUID userId, String username, List<String> roles);
    String generateRefreshToken(UUID userId);
    boolean validateToken(String token);
    UUID getUserIdFromToken(String token);
    String getEmailFromToken(String token);
    List<String> getRolesFromToken(String token);
    String getTokenType(String token);
    boolean isTokenExpired(String token);
    String hashToken(String token);
}
