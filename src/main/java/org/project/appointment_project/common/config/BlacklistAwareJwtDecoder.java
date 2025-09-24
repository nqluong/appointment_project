package org.project.appointment_project.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.auth.service.TokenStatusChecker;
import org.project.appointment_project.common.security.jwt.service.JwtClaimsExtractor;
import org.project.appointment_project.common.security.jwt.validator.TokenValidator;
import org.project.appointment_project.user.model.User;
import org.project.appointment_project.user.repository.UserRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class BlacklistAwareJwtDecoder implements JwtDecoder {
    private final JwtDecoder jwtDecoder;
    private final TokenValidator tokenValidator;
    private final TokenStatusChecker tokenStatusChecker;
    private final UserRepository userRepository;

    @Override
    public Jwt decode(String token) throws JwtException {
        Jwt jwt = jwtDecoder.decode(token);



        // Kiểm tra token có trong blacklist không
        String tokenHash = tokenValidator.hash(token);
        if (tokenStatusChecker.isTokenInvalidated(tokenHash)) {
            throw new JwtException("Token has been invalidated");
        }

        if (!isTokenValidBySecurityTimestamp(token)) {
            log.debug("Token rejected: invalidated by user security timestamp");
            throw new JwtException("Token has been invalidated by security policy");
        }

        return jwt;
    }

    private boolean isTokenValidBySecurityTimestamp(String token) {
        try {
            UUID userId = tokenValidator.getUserId(token);
            LocalDateTime tokenIssuedAt = getTokenIssuedTime(token);

            if (userId == null || tokenIssuedAt == null) {
                log.warn("Cannot validate token: missing userId or issuedAt");
                return false;
            }

            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                log.warn("Token validation failed: user not found for ID: {}", userId);
                return false;
            }

            if (!user.isActive() || user.getDeletedAt() != null) {
                log.debug("Token rejected: user account is inactive or deleted");
                return false;
            }

            if (user.getTokensInvalidBefore() != null &&
                    tokenIssuedAt.isBefore(user.getTokensInvalidBefore())) {
                log.debug("Token rejected: issued before security invalidation timestamp. " +
                                "Token issued: {}, Invalid before: {}",
                        tokenIssuedAt, user.getTokensInvalidBefore());
                return false;
            }

            return true;

        } catch (Exception e) {
            log.error("Error validating token against security timestamp", e);
            return false;
        }
    }

    private LocalDateTime getTokenIssuedTime(String token) {
        try {
            return tokenValidator.getIssuedTime(token);
        } catch (Exception e) {
            log.error("Error getting token issued time", e);
            return null;
        }
    }
}
