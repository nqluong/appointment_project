package org.project.appointment_project.auth.service.impl;

import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.auth.service.UserAuthenticationService;
import org.project.appointment_project.common.exception.CustomException;
import org.project.appointment_project.common.exception.ErrorCode;
import org.project.appointment_project.user.dto.request.AssignRoleRequest;
import org.project.appointment_project.user.dto.request.UpdateRoleExpirationRequest;
import org.project.appointment_project.user.dto.response.RoleInfo;
import org.project.appointment_project.user.model.User;
import org.project.appointment_project.user.repository.UserRepository;
import org.project.appointment_project.user.repository.UserRoleRepositoryJdbcImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserAuthenticationServiceImpl implements UserAuthenticationService {

    UserRepository userRepository;
    PasswordEncoder passwordEncoder;
    UserRoleRepositoryJdbcImpl userRoleRepositoryJdbc;

    @Override
    public User authenticateUser(String username, String password) {
        User user = userRepository.findActiveUserByUsernameWithRoles(username)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }

        if (!user.isEmailVerified()) {
            throw new CustomException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
        return user;
    }


}
