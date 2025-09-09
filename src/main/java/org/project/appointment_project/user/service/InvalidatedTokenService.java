package org.project.appointment_project.user.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.user.enums.TokenType;
import org.project.appointment_project.user.model.InvalidatedToken;
import org.project.appointment_project.user.model.User;
import org.project.appointment_project.user.repository.InvalidatedTokenRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InvalidatedTokenService {
    InvalidatedTokenRepository invalidatedTokenRepository;

    public void invalidateToken(String tokenHash, LocalDateTime expiresAt, User user) {
        InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                .tokenHash(tokenHash)
                .tokenType(TokenType.REFRESH_TOKEN)
                .blackListedAt(LocalDateTime.now())
                .expiresAt(expiresAt)
                .user(user)
                .build();
        invalidatedTokenRepository.save(invalidatedToken);
        log.info("Successfully invalidated refresh token for user: {}", user.getId());
    }
}
