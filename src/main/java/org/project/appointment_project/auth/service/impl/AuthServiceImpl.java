package org.project.appointment_project.auth.service.impl;

import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.auth.dto.request.LoginRequest;
import org.project.appointment_project.auth.dto.request.LogoutRequest;
import org.project.appointment_project.auth.dto.request.RefreshTokenRequest;
import org.project.appointment_project.auth.dto.request.VerifyTokenRequest;
import org.project.appointment_project.auth.dto.response.LoginResponse;
import org.project.appointment_project.auth.dto.response.TokenResponse;
import org.project.appointment_project.auth.dto.response.VerifyTokenResponse;
import org.project.appointment_project.auth.service.*;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthServiceImpl implements AuthService {

    AuthenticationManager authenticationManager;
    TokenManager tokenManager;
    SessionManager sessionManager;
    TokenVerificationService tokenVerificationService;

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        return authenticationManager.authenticate(loginRequest);
    }


    @Override
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        return tokenManager.refreshAccessToken(request);
    }

    @Override
    public void logout(LogoutRequest request) {
        sessionManager.terminateSession(request);
    }

    @Override
    public VerifyTokenResponse verifyToken(VerifyTokenRequest request) {
        return tokenVerificationService.verifyToken(request);
    }
}
