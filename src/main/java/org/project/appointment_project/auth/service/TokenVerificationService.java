package org.project.appointment_project.auth.service;

import org.project.appointment_project.auth.dto.request.VerifyTokenRequest;
import org.project.appointment_project.auth.dto.response.VerifyTokenResponse;

// Check verify token
public interface TokenVerificationService {
    VerifyTokenResponse verifyToken(VerifyTokenRequest token);
}
