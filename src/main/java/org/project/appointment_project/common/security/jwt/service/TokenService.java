package org.project.appointment_project.common.security.jwt.service;

import lombok.Data;
import org.project.appointment_project.auth.dto.TokenPair;
import org.project.appointment_project.auth.dto.TokenResponse;
import org.project.appointment_project.user.enums.RoleName;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public interface TokenService {
    TokenResponse generateTokens(UUID userId, String username, List<RoleName> roles);

    boolean validateToken(String token);

    UUID getUserIdFromToken(String token);
    String getTokenType(String token);
    LocalDateTime getExpirationTimeFromToken(String token);

    boolean isTokenExpired(String token);
    String hashToken(String token);
}
