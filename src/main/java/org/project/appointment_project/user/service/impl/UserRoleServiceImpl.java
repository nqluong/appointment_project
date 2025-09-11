package org.project.appointment_project.user.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.common.exception.CustomException;
import org.project.appointment_project.common.exception.ErrorCode;
import org.project.appointment_project.user.model.User;
import org.project.appointment_project.user.repository.UserRepository;
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

    @Override
    public List<String> getUserRoleNames(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return user.getUserRoles().stream()
                .filter(userRole -> userRole.isActive())
                .map(userRole -> userRole.getRole().getName())
                .distinct()
                .toList();
    }
}
