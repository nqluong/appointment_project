package org.project.appointment_project.auth.service;

import org.project.appointment_project.auth.dto.request.RefreshTokenRequest;
import org.project.appointment_project.auth.dto.response.TokenResponse;

public interface TokenManager {
    TokenResponse refreshAccessToken(RefreshTokenRequest request);
}
