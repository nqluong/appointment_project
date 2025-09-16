package org.project.appointment_project.user.service;

import org.project.appointment_project.common.dto.PageResponse;
import org.project.appointment_project.user.dto.request.UserSearchRequest;
import org.project.appointment_project.user.dto.response.UserDeletionResponse;
import org.project.appointment_project.user.dto.response.UserResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserService {

    UserDeletionResponse deleteUser(UUID userId, UUID deletedBy, String reason, boolean hardDelete);

    UserResponse restoreUser(UUID userId, UUID restoredBy, String reason);

    PageResponse<UserResponse> getUsers(UserSearchRequest request, Pageable pageable);

    PageResponse<UserResponse> getDeletedUsers(String userType, Pageable pageable);

    PageResponse<UserResponse> getActiveUsers(String userType, Pageable pageable);

    UserResponse getUserById(UUID userId, boolean includeDeleted);

    boolean canDeleteUser(UUID userId);
}
