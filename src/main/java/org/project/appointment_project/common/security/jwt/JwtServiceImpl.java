package org.project.appointment_project.common.security.jwt;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.common.exception.CustomException;
import org.project.appointment_project.common.exception.ErrorCode;
import org.project.appointment_project.user.model.InvalidatedToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JwtServiceImpl implements JwtService {

    @Value("${jwt.signer-key}")
    String jwtSecret;

    @Value("${jwt.expiration}")
    Long accessTokenExpiration;

    @Value("${jwt.refresh.expiration}")
    Long refreshTokenExpiration;

    InvalidatedToken invalidatedToken;

    @Override
    public String generateAccessToken(UUID userId, String username, List<String> roles) {
        try{
            return createToken(userId, username, roles, accessTokenExpiration);
        }catch (Exception e){
            throw new CustomException(ErrorCode.TOKEN_GENERATION_FAILED);
        }
    }

    @Override
    public String generateRefreshToken(UUID userId) {
        try{
            return createRefreshToken(userId, refreshTokenExpiration);
        }catch (Exception e){
            throw new CustomException(ErrorCode.TOKEN_GENERATION_FAILED);
        }
    }

    @Override
    public boolean validateToken(String token) {
        try {
            verifyTokenInternal(token, false);
            return true;
        } catch (Exception e) {
            log.debug("Token verification failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public UUID getUserIdFromToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            String userIdStr = signedJWT.getJWTClaimsSet().getStringClaim("userId");
            return UUID.fromString(userIdStr);
        } catch (ParseException e) {
            log.error("Error parsing token to get user ID", e);
            throw new CustomException(ErrorCode.TOKEN_PARSE_ERROR, e);
        }
    }

    @Override
    public String getEmailFromToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            return signedJWT.getJWTClaimsSet().getStringClaim("email");
        } catch (ParseException e) {
            log.error("Error parsing token to get email", e);
            throw new CustomException(ErrorCode.TOKEN_PARSE_ERROR, e);
        }
    }

    @Override
    public List<String> getRolesFromToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            return signedJWT.getJWTClaimsSet().getStringListClaim("roles");
        } catch (ParseException e) {
            log.error("Error parsing token to get roles", e);
            return List.of();
        }
    }

    @Override
    public String getTokenType(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            // Check if token has subject (access token) or not (refresh token)
            String subject = signedJWT.getJWTClaimsSet().getSubject();
            return (subject != null) ? "ACCESS" : "REFRESH";
        } catch (ParseException e) {
            log.error("Error parsing token to get type", e);
            return "UNKNOWN";
        }
    }

    @Override
    public boolean isTokenExpired(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
            Date currentTime = new Date();

            log.debug("Checking token expiration - Current: {}, Expiry: {}", currentTime, expirationTime);
            return expirationTime.before(currentTime);
        } catch (ParseException e) {
            log.error("Error parsing token to check expiration", e);
            return true; // Consider expired if can't parse
        }
    }

    @Override
    public String hashToken(String token) {
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

    private String createToken(UUID userId, String username, List<String> roles, Long expiration) {
        try {
            JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);
            JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                    .subject(username)
                    .issuer("appointment")
                    .issueTime(new Date())
                    .expirationTime(new Date(Instant.now().plus(expiration, ChronoUnit.SECONDS).toEpochMilli()))
                    .jwtID(UUID.randomUUID().toString())
                    .claim("userId",userId.toString())
                    .claim("roles", roles)
                    .build();

            Payload payload = new Payload(jwtClaimsSet.toJSONObject());
            JWSObject jwsObject = new JWSObject(header, payload);
            jwsObject.sign(new MACSigner(jwtSecret.getBytes()));

            return jwsObject.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("Error generating token", e);
        }
    }

    private String createRefreshToken(UUID userId, Long expiration) {
        try {

            JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

            JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                    .issuer("appointment")
                    .issueTime(new Date())
                    .expirationTime(new Date(
                            Instant.now().plus(expiration, ChronoUnit.SECONDS).toEpochMilli()
                    ))
                    .jwtID(UUID.randomUUID().toString())
                    .claim("userId",userId.toString())
                    //   .claim("isActive", user.getIsActive())
                    .build();

            Payload payload = new Payload(jwtClaimsSet.toJSONObject());
            JWSObject jwsObject = new JWSObject(header, payload);
            jwsObject.sign(new MACSigner(jwtSecret.getBytes()));

            return jwsObject.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("Error generating token", e);
        }
    }

    private SignedJWT verifyTokenInternal(String token, boolean isRefresh) throws JOSEException, ParseException {

        JWSVerifier verifier = new MACVerifier(jwtSecret.getBytes());
        SignedJWT signedJWT = SignedJWT.parse(token);
        String userIdStr = signedJWT.getJWTClaimsSet().getStringClaim("userId");
        Date expiryTime = (isRefresh)
                ? new Date(signedJWT
                .getJWTClaimsSet()
                .getIssueTime()
                .toInstant()
                .plus(refreshTokenExpiration, ChronoUnit.SECONDS)
                .toEpochMilli())
                : signedJWT.getJWTClaimsSet().getExpirationTime();

        boolean verified = signedJWT.verify(verifier);
        if (!(verified && expiryTime.after(new Date()))) {
            throw new CustomException(ErrorCode.UNAUTHENTICATED);
        }

        return signedJWT;
    }
}
