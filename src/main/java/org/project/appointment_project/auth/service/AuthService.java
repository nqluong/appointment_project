package org.project.appointment_project.auth.service;

import org.project.appointment_project.auth.dto.*;

public interface AuthService {
    LoginResponse login(LoginRequest loginRequest);
    TokenResponse refreshToken(RefreshTokenRequest request);

}
