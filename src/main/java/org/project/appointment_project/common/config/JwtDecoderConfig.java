package org.project.appointment_project.common.config;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.auth.service.TokenStatusChecker;
import org.project.appointment_project.common.security.jwt.validator.TokenValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.spec.SecretKeySpec;

//Cau hinh jwt Decoder
@Configuration
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JwtDecoderConfig {
    @Value("${jwt.signer-key}")
    String signerKey;

    final TokenValidator tokenValidator;
    final TokenStatusChecker tokenStatusChecker;

    @Bean
    public JwtDecoder baseJwtDecoder() {

        SecretKeySpec secretKeySpec = new SecretKeySpec(signerKey.getBytes(), "HS512");
        return NimbusJwtDecoder.withSecretKey(secretKeySpec)
                .macAlgorithm(MacAlgorithm.HS512)
                .build();
    }

    @Bean
    @Primary
    public JwtDecoder jwtDecoder() {
        return new BlacklistAwareJwtDecoder(baseJwtDecoder(), tokenValidator, tokenStatusChecker);
    }
}
