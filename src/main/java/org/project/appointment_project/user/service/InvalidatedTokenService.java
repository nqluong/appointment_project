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

    /**
     * Invalidate token bằng cách lưu token hash vào db
     * @param tokenHash - Hash của token
     * @param expiresAt - Thời gian hết hạn của token
     * @param user - User sở hữu token
     * @param tokenType - Loại token (ACCESS_TOKEN hoặc REFRESH_TOKEN)
     */
    public void invalidateToken(String tokenHash, LocalDateTime expiresAt, User user, TokenType tokenType) {
        InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                .tokenHash(tokenHash)
                .tokenType(tokenType)
                .blackListedAt(LocalDateTime.now())
                .expiresAt(expiresAt)
                .user(user)
                .build();

        invalidatedTokenRepository.save(invalidatedToken);
        log.info("Successfully invalidated {} token for user: {}", tokenType.name(), user.getId());
    }
}
