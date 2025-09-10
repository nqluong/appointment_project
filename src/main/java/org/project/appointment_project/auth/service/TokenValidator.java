package org.project.appointment_project.auth.service;

import org.project.appointment_project.auth.dto.response.TokenValidationResult;
import org.project.appointment_project.user.enums.TokenType;

public interface TokenValidator {
    TokenValidationResult validateToken(String token, TokenType type);
}
