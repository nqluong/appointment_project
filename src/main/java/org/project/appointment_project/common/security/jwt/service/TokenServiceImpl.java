package org.project.appointment_project.common.security.jwt.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.auth.dto.response.TokenResponse;
import org.project.appointment_project.common.exception.CustomException;
import org.project.appointment_project.common.exception.ErrorCode;
import org.project.appointment_project.common.security.jwt.generator.AccessTokenGenerator;
import org.project.appointment_project.common.security.jwt.generator.RefreshTokenGenerator;
import org.project.appointment_project.common.security.jwt.validator.TokenValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TokenServiceImpl implements TokenService {
    @Value("${jwt.signer-key}")
    String jwtSecret;

    @Value("${jwt.expiration}")
    Long accessTokenExpiration;

    @Value("${jwt.refresh.expiration}")
    Long refreshTokenExpiration;

    final AccessTokenGenerator accessTokenGenerator;
    final RefreshTokenGenerator refreshTokenGenerator;
    final TokenValidator tokenValidator;

    @Override
    public TokenResponse generateTokens(UUID userId, String username, String email, List<String> roles) {
        try {
            String accessToken = accessTokenGenerator.generate(userId, username, email,roles, accessTokenExpiration);
            String refreshToken = refreshTokenGenerator.generate(userId, refreshTokenExpiration);

            return TokenResponse.builder().accessToken(accessToken).refreshToken(refreshToken).build();
        } catch (Exception e) {
            throw new CustomException(ErrorCode.TOKEN_GENERATION_FAILED);
        }
    }

    @Override
    public boolean validateToken(String token) {
        return tokenValidator.validate(token);
    }

    @Override
    public UUID getUserIdFromToken(String token) {
        return tokenValidator.getUserId(token);
    }

    @Override
    public LocalDateTime getExpirationTimeFromToken(String token) {
        return tokenValidator.getExpiretionTime(token);
    }

    @Override
    public String getTokenType(String token) {
        return tokenValidator.getTokenType(token);
    }

    @Override
    public boolean isTokenExpired(String token) {
        return tokenValidator.isExpired(token);
    }

    @Override
    public String hashToken(String token) {
        return tokenValidator.hash(token);
    }
}
