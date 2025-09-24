package org.project.appointment_project.auth.service.impl;

import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.auth.dto.request.*;
import org.project.appointment_project.auth.dto.response.*;
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
    PasswordResetService passwordResetService;

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

    @Override
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        return passwordResetService.forgotPassword(request);
    }

    @Override
    public PasswordResetResponse resetPassword(PasswordResetRequest request) {
        return passwordResetService.passwordReset(request);
    }

    @Override
    public boolean validateResetToken(String token) {
        return passwordResetService.validateResetToken(token);
    }
}
