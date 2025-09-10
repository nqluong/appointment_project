package org.project.appointment_project.auth.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.auth.dto.request.RefreshTokenRequest;
import org.project.appointment_project.auth.dto.response.TokenResponse;
import org.project.appointment_project.auth.dto.response.TokenValidationResult;
import org.project.appointment_project.auth.service.TokenInvalidator;
import org.project.appointment_project.auth.service.TokenManager;
import org.project.appointment_project.auth.service.TokenValidator;
import org.project.appointment_project.auth.service.UserAuthenticationService;
import org.project.appointment_project.common.exception.CustomException;
import org.project.appointment_project.common.exception.ErrorCode;
import org.project.appointment_project.common.security.jwt.service.TokenService;
import org.project.appointment_project.user.enums.TokenType;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StandardTokenManager implements TokenManager {
    TokenValidator tokenValidator;
    TokenInvalidator tokenInvalidator;
    TokenService tokenService;
    UserAuthenticationService userAuthenticationService;

    @Override
    public TokenResponse refreshAccessToken(RefreshTokenRequest request) {
        try {
            TokenValidationResult validationResult = validateRefreshToken(request.getRefreshToken());
            invalidateOldRefreshToken(validationResult);

            return generateNewTokens(validationResult);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("Token refresh failed", e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "Token refresh failed", e);
        }
    }

    private TokenValidationResult validateRefreshToken(String refreshToken) {
        return tokenValidator.validateToken(refreshToken, TokenType.REFRESH_TOKEN);
    }

    private void invalidateOldRefreshToken(TokenValidationResult validationResult) {
        tokenInvalidator.invalidateToken(
                validationResult.getTokenHash(),
                validationResult.getExpirationTime(),
                validationResult.getUser(),
                TokenType.REFRESH_TOKEN
        );
    }

    private TokenResponse generateNewTokens(TokenValidationResult validationResult) {
        List<String> roles = userAuthenticationService.getUserRoles(validationResult.getUserId());
        return tokenService.generateTokens(
                validationResult.getUserId(),
                validationResult.getUser().getUsername(),
                validationResult.getUser().getEmail(),
                roles
        );
    }
}
