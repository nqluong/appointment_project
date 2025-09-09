package org.project.appointment_project.auth.service;

import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.auth.dto.*;
import org.project.appointment_project.auth.mapper.AuthMapper;
import org.project.appointment_project.common.exception.CustomException;
import org.project.appointment_project.common.exception.ErrorCode;
import org.project.appointment_project.common.security.jwt.service.TokenService;
import org.project.appointment_project.user.enums.RoleName;
import org.project.appointment_project.user.enums.TokenType;
import org.project.appointment_project.user.model.InvalidatedToken;
import org.project.appointment_project.user.model.User;
import org.project.appointment_project.user.repository.InvalidatedTokenRepository;
import org.project.appointment_project.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthServiceImpl implements AuthService {

    UserAuthenticationService userAuthenticationService;
    InvalidatedTokenRepository invalidatedTokenRepository;
    TokenService tokenService;
    AuthMapper authMapper;
    private final UserRepository userRepository;

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        try {
            User user = userAuthenticationService.authenticateUser(loginRequest.getUsername(), loginRequest.getPassword());

            List<RoleName> roles = userAuthenticationService.getUserRoles(user.getId());

            TokenResponse tokenResponse = tokenService.generateTokens(user.getId(), user.getUsername(), roles);

            return LoginResponse.builder()
                    .accessToken(tokenResponse.getAccessToken())
                    .refreshToken(tokenResponse.getRefreshToken())
                    .email(user.getEmail())
                    .userId(user.getId())
                    .build();

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error during login for user: {}", loginRequest.getUsername(), e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "Login failed", e);
        }
    }


    @Override
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        try {
            if (!tokenService.validateToken(refreshToken)) {
                throw new CustomException(ErrorCode.TOKEN_INVALID, "Invalid refresh token");
            }
            if (tokenService.isTokenExpired(refreshToken)) {
                throw new CustomException(ErrorCode.TOKEN_EXPIRED, "Refresh token has expired");
            }
            String tokenType = tokenService.getTokenType(refreshToken);
            if (!"REFRESH".equalsIgnoreCase(tokenType)) {
                throw new CustomException(ErrorCode.TOKEN_INVALID, "Provided token is not a refresh token");
            }
            String tokenHash = tokenService.hashToken(refreshToken);
            if (invalidatedTokenRepository.existsByTokenHash(tokenHash)) {
                throw new CustomException(ErrorCode.TOKEN_EXPIRED, "Refresh token has expired");
            }

            UUID userId = tokenService.getUserIdFromToken(refreshToken);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND, "User not found for the provided token"));
            List<RoleName> roles = userAuthenticationService.getUserRoles(userId);

            InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                    .tokenHash(tokenHash)
                    .tokenType(TokenType.REFRESH_TOKEN)
                    .blackListedAt(LocalDateTime.now())
                    .expiresAt(tokenService.getExpirationTimeFromToken(refreshToken))
                    .user(user)
                    .build();

            invalidatedTokenRepository.save(invalidatedToken);

            return tokenService.generateTokens(userId, user.getUsername(), roles);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
