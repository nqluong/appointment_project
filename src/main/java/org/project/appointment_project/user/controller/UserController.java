package org.project.appointment_project.user.controller;

import lombok.RequiredArgsConstructor;
import org.project.appointment_project.common.dto.PageResponse;
import org.project.appointment_project.common.util.SecurityUtils;
import org.project.appointment_project.user.dto.request.UserSearchRequest;
import org.project.appointment_project.user.dto.response.UserDeletionResponse;
import org.project.appointment_project.user.dto.response.UserResponse;
import org.project.appointment_project.user.service.UserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final SecurityUtils securityUtils;

    @GetMapping
    public ResponseEntity<PageResponse<UserResponse>> getUsers(
            @RequestParam(required = false) String userType,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isDeleted,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        UserSearchRequest request = UserSearchRequest.builder()
                .userType(userType)
                .keyword(keyword)
                .isDeleted(isDeleted)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<UserResponse> response = userService.getUsers(request, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/deleted")
    public ResponseEntity<PageResponse<UserResponse>> getDeletedUsers(
            @RequestParam(required = false) String userType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);

        PageResponse<UserResponse> response = userService.getDeletedUsers(userType, pageable);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/active")
    public ResponseEntity<PageResponse<UserResponse>> getActiveUsers(
            @RequestParam(required = false) String userType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);

        PageResponse<UserResponse> response = userService.getActiveUsers(userType, pageable);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDeletionResponse> deleteUser(
            @PathVariable UUID userId,
            @RequestParam(required = false) String reason,
            @RequestParam(defaultValue = "false") Boolean hardDelete) {

        UUID deletedBy = securityUtils.getCurrentUserId();

        UserDeletionResponse response = userService.deleteUser(userId, deletedBy, reason, hardDelete);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{userId}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> restoreUser(
            @PathVariable UUID userId,
            @RequestParam(required = false) String reason) {

        UUID restoredBy = securityUtils.getCurrentUserId();

        UserResponse response = userService.restoreUser(userId, restoredBy, reason);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "false") boolean includeDeleted) {

        UserResponse user = userService.getUserById(userId, includeDeleted);
        return ResponseEntity.ok(user);
    }


    @GetMapping("/{userId}/can-delete")
    public ResponseEntity<Boolean> canDeleteUser(@PathVariable UUID userId) {
        boolean canDelete = userService.canDeleteUser(userId);
        return ResponseEntity.ok(canDelete);
    }
}
