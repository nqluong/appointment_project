package org.project.appointment_project.auth.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.auth.dto.request.LoginRequest;
import org.project.appointment_project.auth.dto.response.LoginResponse;
import org.project.appointment_project.auth.dto.response.TokenResponse;
import org.project.appointment_project.auth.service.AuthenticationManager;
import org.project.appointment_project.auth.service.UserAuthenticationService;
import org.project.appointment_project.common.exception.CustomException;
import org.project.appointment_project.common.exception.ErrorCode;
import org.project.appointment_project.common.security.jwt.service.TokenService;
import org.project.appointment_project.user.model.User;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StandardAuthenticationManager implements AuthenticationManager {

    UserAuthenticationService userAuthenticationService;
    TokenService tokenService;

    @Override
    public LoginResponse authenticate(LoginRequest loginRequest) {
        try {
            User user = authenticateUser(loginRequest);
            List<String> roles = getUserRoles(user);
            TokenResponse tokenResponse = generateTokens(user, roles);

            return buildLoginResponse(user, tokenResponse);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("Authentication failed for user: {}", loginRequest.getUsername(), e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "Authentication failed", e);
        }
    }

    private User authenticateUser(LoginRequest loginRequest) {
        return userAuthenticationService.authenticateUser(loginRequest.getUsername(), loginRequest.getPassword());
    }

    private List<String> getUserRoles(User user) {
        return userAuthenticationService.getUserRoles(user.getId());
    }

    private TokenResponse generateTokens(User user, List<String> roles) {
        return tokenService.generateTokens(user.getId(), user.getUsername(), user.getEmail(),roles);
    }

    private LoginResponse buildLoginResponse(User user, TokenResponse tokenResponse) {
        return LoginResponse.builder()
                .accessToken(tokenResponse.getAccessToken())
                .refreshToken(tokenResponse.getRefreshToken())
                .email(user.getEmail())
                .userId(user.getId())
                .build();
    }
}
