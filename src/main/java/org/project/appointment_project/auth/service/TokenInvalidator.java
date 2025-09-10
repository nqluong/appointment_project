package org.project.appointment_project.auth.service;

import org.project.appointment_project.user.enums.TokenType;
import org.project.appointment_project.user.model.User;

import java.time.LocalDateTime;

public interface TokenInvalidator {
    void invalidateToken(String tokenHash, LocalDateTime expiration, User user, TokenType type);
}
