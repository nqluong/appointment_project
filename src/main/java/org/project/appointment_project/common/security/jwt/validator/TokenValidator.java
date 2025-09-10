package org.project.appointment_project.common.security.jwt.validator;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.SignedJWT;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.common.exception.CustomException;
import org.project.appointment_project.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TokenValidator {
    @Value("${jwt.signer-key}")
    String jwtSecret;

    @Value("${jwt.refresh.expiration}")
    Long refreshTokenExpiration;

    public boolean validate(String token) {
        try {
            verifyTokenInternal(token, false);
            return true;
        } catch (Exception e) {
            log.debug("Token verification failed: {}", e.getMessage());
            return false;
        }
    }

    public UUID getUserId(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            String userIdStr = signedJWT.getJWTClaimsSet().getStringClaim("userId");
            return UUID.fromString(userIdStr);
        } catch (ParseException e) {
            log.error("Error parsing token to get user ID", e);
            throw new CustomException(ErrorCode.TOKEN_PARSE_ERROR, e);
        }
    }

    public String getEmail(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            return signedJWT.getJWTClaimsSet().getStringClaim("email");
        } catch (ParseException e) {
            log.error("Error parsing token to get email", e);
            throw new CustomException(ErrorCode.TOKEN_PARSE_ERROR, e);
        }
    }

    public String getUsername(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            return signedJWT.getJWTClaimsSet().getSubject();
        } catch (ParseException e) {
            log.error("Error parsing token to get username", e);
            throw new CustomException(ErrorCode.TOKEN_PARSE_ERROR, e);
        }
    }

    public List<String> getRoles(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            return signedJWT.getJWTClaimsSet().getStringListClaim("roles");
        } catch (ParseException e) {
            log.error("Error parsing token to get roles", e);
            return List.of();
        }
    }

    public LocalDateTime getExpiretionTime(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            Date exp = signedJWT.getJWTClaimsSet().getExpirationTime();
            if (exp == null) {
                log.error("Token does not contain expiration time");
                throw new CustomException(ErrorCode.TOKEN_INVALID, "Token lacks expiration time");
            }
            return LocalDateTime.ofInstant(exp.toInstant(), ZoneId.systemDefault()).truncatedTo(ChronoUnit.SECONDS);
        } catch (ParseException e) {
            log.error("Error parsing token to get expiration time", e);
            throw new CustomException(ErrorCode.TOKEN_PARSE_ERROR, e);
        }
    }

    public String getTokenType(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            String subject = signedJWT.getJWTClaimsSet().getSubject();
            return (subject != null) ? "ACCESS" : "REFRESH";
        } catch (ParseException e) {
            log.error("Error parsing token to get type", e);
            return "UNKNOWN";
        }
    }

    public boolean isExpired(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
            Date currentTime = new Date();
            return expirationTime.before(currentTime);
        } catch (ParseException e) {
            log.error("Error parsing token to check expiration", e);
            return true;
        }
    }

    public String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    private SignedJWT verifyTokenInternal(String token, boolean isRefresh) throws JOSEException, ParseException {
        JWSVerifier verifier = new MACVerifier(jwtSecret.getBytes());
        SignedJWT signedJWT = SignedJWT.parse(token);

        Date expiryTime = isRefresh
                ? new Date(signedJWT.getJWTClaimsSet().getIssueTime().toInstant()
                .plus(refreshTokenExpiration, ChronoUnit.SECONDS).toEpochMilli())
                : signedJWT.getJWTClaimsSet().getExpirationTime();

        boolean verified = signedJWT.verify(verifier);
        if (!(verified && expiryTime.after(new Date()))) {
            throw new CustomException(ErrorCode.UNAUTHENTICATED);
        }

        return signedJWT;
    }
}
