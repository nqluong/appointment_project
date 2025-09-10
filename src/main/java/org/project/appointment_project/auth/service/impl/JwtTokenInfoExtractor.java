package org.project.appointment_project.auth.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.auth.dto.response.TokenInfo;
import org.project.appointment_project.auth.service.TokenInfoExactor;
import org.project.appointment_project.common.exception.CustomException;
import org.project.appointment_project.common.exception.ErrorCode;
import org.project.appointment_project.common.security.jwt.validator.TokenValidator;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class JwtTokenInfoExtractor implements TokenInfoExactor {
    TokenValidator tokenValidator;

    @Override
    public TokenInfo extractTokenInfo(String token) {
        try {
            return TokenInfo.builder()
                    .userId(tokenValidator.getUserId(token))
                    .username(tokenValidator.getUsername(token))
                    .email(tokenValidator.getEmail(token))
                    .roles(tokenValidator.getRoles(token))
                    .tokenType(tokenValidator.getTokenType(token))
                    .expirationTime(tokenValidator.getExpiretionTime(token))
                    .isExpired(tokenValidator.isExpired(token))
                    .build();
        } catch (CustomException e) {
            log.error("Failed to extract token info", e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during token info extraction", e);
            throw new CustomException(ErrorCode.TOKEN_PARSE_ERROR, "Failed to extract token information", e);
        }
    }
}
