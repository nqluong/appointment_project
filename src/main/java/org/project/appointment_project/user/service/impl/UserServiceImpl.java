package org.project.appointment_project.user.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.common.dto.PageResponse;
import org.project.appointment_project.common.exception.CustomException;
import org.project.appointment_project.common.exception.ErrorCode;
import org.project.appointment_project.common.mapper.PageMapper;
import org.project.appointment_project.user.dto.request.UserSearchRequest;
import org.project.appointment_project.user.dto.response.UserDeletionResponse;
import org.project.appointment_project.user.dto.response.UserResponse;
import org.project.appointment_project.user.mapper.UserMapper;
import org.project.appointment_project.user.model.User;
import org.project.appointment_project.user.repository.UserRepository;
import org.project.appointment_project.user.service.UserService;
import org.project.appointment_project.user.service.strategy.UserDeletionStrategy;
import org.project.appointment_project.user.service.strategy.UserDeletionStrategyFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserServiceImpl implements UserService {

    UserRepository userRepository;
    UserDeletionStrategyFactory userDeletionStrategyFactory;
    UserMapper userMapper;
    PageMapper pageMapper;

    @Override
    @Transactional
    public UserDeletionResponse deleteUser(UUID userId, UUID deletedBy, String reason, boolean hardDelete) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        UserDeletionStrategy strategy = userDeletionStrategyFactory.getStrategy(user, hardDelete);

        if (!strategy.deleteUser(user)) {
            throw new CustomException(ErrorCode.USER_CANNOT_BE_DELETED);
        }

        String userType = getUserType(user);
        boolean isHardDelete = strategy.getStrategyName().contains("HARD");

        strategy.delete(user, deletedBy, reason);

        return UserDeletionResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .userType(userType)
                .hardDeleted(isHardDelete)
                .deletedAt(isHardDelete ? LocalDateTime.now() : user.getDeletedAt())
                .message(String.format("User %s successfully", isHardDelete ? "deleted permanently" : "deactivated"))
                .build();
    }

    @Override
    @Transactional
    public UserResponse restoreUser (UUID userId, UUID restoredBy, String reason)  {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (user.getDeletedAt() == null) {
            throw new CustomException(ErrorCode.USER_NOT_DELETED);
        }

        user.setDeletedAt(null);
        user.setDeletedBy(null);
        user.setActive(true);

        if (user.getMedicalProfile() != null) {
            user.getMedicalProfile().setDoctorApproved(true);
        }

        user.getUserRoles().forEach(userRole -> userRole.setActive(true));

        userRepository.save(user);

        log.info("User restored: {} by: {} for reason: {}",
                user.getId(), restoredBy, reason);

        return userMapper.toResponse(user);
    }

    @Override
    public PageResponse<UserResponse> getUsers(UserSearchRequest request, Pageable pageable) {
        Page<User> userPage;

        if (request.getIsDeleted() != null && request.getIsDeleted()) {
            if ("DOCTOR".equalsIgnoreCase(request.getUserType())) {
                userPage = userRepository.findDeletedDoctors(pageable);
            } else if ("PATIENT".equalsIgnoreCase(request.getUserType())) {
                userPage = userRepository.findDeletedPatients(pageable);
            } else {
                userPage = userRepository.findAllDeletedUsers(pageable);
            }
        } else {
            if ("DOCTOR".equalsIgnoreCase(request.getUserType())) {
                userPage = userRepository.findActiveDoctors(pageable);
            } else if ("PATIENT".equalsIgnoreCase(request.getUserType())) {
                userPage = userRepository.findActivePatients(pageable);
            } else {
                userPage = userRepository.findAllActiveUsers(pageable);
            }
        }

        if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
            userPage = userRepository.searchUsers(
                    request.getKeyword(),
                    request.getUserType(),
                    request.getIsDeleted(),
                    pageable
            );
        }

        return pageMapper.toPageResponse(userPage, userMapper::toResponse);
    }

    @Override
    public PageResponse<UserResponse> getDeletedUsers(String userType, Pageable pageable) {
        Page<User> userPage;

        if ("DOCTOR".equalsIgnoreCase(userType)) {
            userPage = userRepository.findDeletedDoctors(pageable);
        } else if ("PATIENT".equalsIgnoreCase(userType)) {
            userPage = userRepository.findDeletedPatients(pageable);
        } else {
            userPage = userRepository.findAllDeletedUsers(pageable);
        }

        return pageMapper.toPageResponse(userPage, userMapper::toResponse);
    }

    @Override
    public PageResponse<UserResponse> getActiveUsers(String userType, Pageable pageable) {
        Page<User> userPage;

        if ("DOCTOR".equalsIgnoreCase(userType)) {
            userPage = userRepository.findActiveDoctors(pageable);
        } else if ("PATIENT".equalsIgnoreCase(userType)) {
            userPage = userRepository.findActivePatients(pageable);
        } else {
            userPage = userRepository.findAllActiveUsers(pageable);
        }

        return pageMapper.toPageResponse(userPage, userMapper::toResponse);
    }

    @Override
    public UserResponse getUserById(UUID userId, boolean includeDeleted) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!includeDeleted && user.getDeletedAt() != null) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        return userMapper.toResponse(user);
    }

    @Override
    public boolean canDeleteUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        UserDeletionStrategy strategy = userDeletionStrategyFactory.getStrategy(user, false);
        return strategy.deleteUser(user);
    }

    private String getUserType(User user) {
        return user.getUserRoles().stream()
                .map(userRole -> userRole.getRole().getName())
                .filter(roleName -> "DOCTOR".equals(roleName) || "PATIENT".equals(roleName))
                .findFirst()
                .orElse("USER");
    }
}
