package org.project.appointment_project.auth.service;

import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.auth.dto.*;
import org.project.appointment_project.auth.mapper.AuthMapper;
import org.project.appointment_project.common.exception.CustomException;
import org.project.appointment_project.common.exception.ErrorCode;
import org.project.appointment_project.common.security.jwt.JwtService;
import org.project.appointment_project.common.security.jwt.service.TokenService;
import org.project.appointment_project.user.enums.RoleName;
import org.project.appointment_project.user.model.Role;
import org.project.appointment_project.user.model.User;
import org.project.appointment_project.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthServiceImpl implements AuthService {

    UserAuthenticationService userAuthenticationService;
    TokenService tokenService;
    AuthMapper authMapper;

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        try{
            User user = userAuthenticationService.authenticateUser(loginRequest.getUsername(), loginRequest.getPassword());

            List<RoleName> roles = userAuthenticationService.getUserRoles(user.getId());

            TokenPair tokenPair = tokenService.generateTokens(user.getId(), user.getUsername(), roles);

            return LoginResponse.builder()
                    .accessToken(tokenPair.getAccessToken())
                    .refreshToken(tokenPair.getRefreshToken())
                    .email(user.getEmail())
                    .userId(user.getId())
                    .build();

        }catch  (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error during login for user: {}", loginRequest.getUsername(), e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "Login failed", e);
        }
    }

    @Override
    public TokenResponse refreshToken(String refreshToken) {
        return null;
    }

    @Override
    public void logout(LogoutRequest logoutRequest) {

    }
}
