package org.project.appointment_project.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.auth.service.TokenStatusChecker;
import org.project.appointment_project.common.security.jwt.service.JwtClaimsExtractor;
import org.project.appointment_project.common.security.jwt.validator.TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class BlacklistAwareJwtDecoder implements JwtDecoder {
    private final JwtDecoder jwtDecoder;
    private final TokenValidator tokenValidator;
    private final TokenStatusChecker tokenStatusChecker;

    @Override
    public Jwt decode(String token) throws JwtException {
        Jwt jwt = jwtDecoder.decode(token);

        // Kiểm tra token có trong blacklist không
        String tokenHash = tokenValidator.hash(token);
        if (tokenStatusChecker.isTokenInvalidated(tokenHash)) {
            throw new JwtException("Token has been invalidated");
        }

        return jwt;
    }
}
