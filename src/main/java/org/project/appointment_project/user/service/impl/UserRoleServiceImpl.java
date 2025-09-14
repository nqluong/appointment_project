package org.project.appointment_project.user.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.common.exception.CustomException;
import org.project.appointment_project.common.exception.ErrorCode;
import org.project.appointment_project.user.repository.UserRepository;
import org.project.appointment_project.user.repository.UserRoleJdbcRepository;
import org.project.appointment_project.user.service.UserRoleService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserRoleServiceImpl implements UserRoleService {

    UserRepository userRepository;
    UserRoleJdbcRepository userRoleRepository;

    @Override
    public List<String> getUserRoleNames(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        return userRoleRepository.getUserRoleNames(userId);
    }

    @Override
    public boolean hasRole(UUID userId, String roleName) {
        List<String> userRoles = getUserRoleNames(userId);
        return userRoles.contains(roleName);
    }

    @Override
    public boolean hasActiveRole(UUID userId, UUID roleId) {
        return userRoleRepository.hasActiveRole(userId, roleId);
    }
}
