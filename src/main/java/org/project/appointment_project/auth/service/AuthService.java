package org.project.appointment_project.auth.service;

import org.project.appointment_project.auth.dto.LoginRequest;
import org.project.appointment_project.auth.dto.LoginResponse;
import org.project.appointment_project.auth.dto.LogoutRequest;
import org.project.appointment_project.auth.dto.TokenResponse;

public interface AuthService {
    LoginResponse login(LoginRequest loginRequest);
    TokenResponse refreshToken(String refreshToken);
    void logout(LogoutRequest logoutRequest);
}
