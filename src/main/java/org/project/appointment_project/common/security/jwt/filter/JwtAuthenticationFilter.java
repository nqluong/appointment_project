package org.project.appointment_project.common.security.jwt.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.common.dto.ErrorResponse;
import org.project.appointment_project.common.exception.ErrorCode;
import org.project.appointment_project.common.security.jwt.service.TokenService;
import org.project.appointment_project.user.repository.InvalidatedTokenRepository;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    TokenService tokenService;
    InvalidatedTokenRepository invalidatedTokenRepository;
    JwtClaimsExtractor jwtClaimExtractor;
    ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try{
            String token = extractToken(request);
            if(token != null && validateToken(token)){
                Authentication authentication = createAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);

            }
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            log.error("Error during JWT authentication: {}", e.getMessage());
            handleAuthenticationError(response, e);
        }

    }

    private void handleAuthenticationError(HttpServletResponse response, Exception e) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .code(ErrorCode.UNAUTHENTICATED.getCode())
                .message(ErrorCode.UNAUTHENTICATED.getMessage())
                .build();
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }


    //Lay token tu header
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    //Kiem tra token co hop le khong
    private boolean validateToken(String token) {
       if(!tokenService.validateToken(token)) {
              return false;
       }

       String hashedToken = tokenService.hashToken(token);
       return !invalidatedTokenRepository.existsByTokenHash(hashedToken);
    }

    private Authentication createAuthentication(String token) {
        UUID userId = tokenService.getUserIdFromToken(token);
        JwtUserPrincipal principal = jwtClaimExtractor.extractPrincipal(token);
        List<GrantedAuthority> authorities = jwtClaimExtractor.extractAuthorities(token);

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        authentication.setDetails(userId);
        return authentication;

    }
}
