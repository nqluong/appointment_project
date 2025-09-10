package org.project.appointment_project.auth.service;

import org.project.appointment_project.auth.dto.request.LoginRequest;
import org.project.appointment_project.auth.dto.request.LogoutRequest;
import org.project.appointment_project.auth.dto.request.RefreshTokenRequest;
import org.project.appointment_project.auth.dto.request.VerifyTokenRequest;
import org.project.appointment_project.auth.dto.response.LoginResponse;
import org.project.appointment_project.auth.dto.response.TokenResponse;
import org.project.appointment_project.auth.dto.response.VerifyTokenResponse;

public interface AuthService {
    LoginResponse login(LoginRequest loginRequest);
    TokenResponse refreshToken(RefreshTokenRequest request);
    void logout(LogoutRequest request);
    VerifyTokenResponse verifyToken(VerifyTokenRequest request);
}
