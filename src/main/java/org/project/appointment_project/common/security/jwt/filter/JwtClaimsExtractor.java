package org.project.appointment_project.common.security.jwt.filter;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.project.appointment_project.common.security.jwt.validator.TokenValidator;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class JwtClaimsExtractor {
    TokenValidator tokenValidator;

    //Lay thong tin user tu token
    public JwtUserPrincipal extractPrincipal(String token) {
        UUID userId = tokenValidator.getUserId(token);
        String username = tokenValidator.getUsername(token);
        List<String> roles = tokenValidator.getRoles(token);

        return JwtUserPrincipal.builder()
                .userId(userId)
                .username(username)
                .roles(roles)
                .build();
    }

    //lay quyen tu token
    public List<GrantedAuthority> extractAuthorities(String token) {
        List<String> roles = tokenValidator.getRoles(token);
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
    }
}
