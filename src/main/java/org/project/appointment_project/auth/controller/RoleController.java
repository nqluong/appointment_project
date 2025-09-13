package org.project.appointment_project.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.project.appointment_project.auth.service.UserAuthenticationService;
import org.project.appointment_project.common.security.annotation.RequireOwnershipOrAdmin;
import org.project.appointment_project.common.security.jwt.principal.JwtUserPrincipal;
import org.project.appointment_project.user.dto.request.AssignRoleRequest;
import org.project.appointment_project.user.dto.request.UpdateRoleExpirationRequest;
import org.project.appointment_project.user.dto.response.RoleInfo;
import org.project.appointment_project.user.service.RoleManagementService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {
    private final RoleManagementService roleManagementService;

    @GetMapping("/user/{userId}")
    @RequireOwnershipOrAdmin
    public ResponseEntity<List<String>> getUserRoles(@PathVariable UUID userId) {
        List<String> roles = roleManagementService.getUserRoles(userId);
        return ResponseEntity.ok(roles);
    }

    @PostMapping("/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> assignRoleToUser(
            @Valid @RequestBody AssignRoleRequest request,
            Authentication authentication) {

        JwtUserPrincipal principal = (JwtUserPrincipal) authentication.getPrincipal();
        UUID assignedBy = principal.getUserId();

        roleManagementService.assignRoleToUser(request, assignedBy);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/revoke/{userId}/{roleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> revokeRoleFromUser(
            @PathVariable UUID userId,
            @PathVariable UUID roleId) {

        roleManagementService.revokeRoleFromUser(userId, roleId);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/revoke-all/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> revokeAllUserRoles(@PathVariable UUID userId) {
        roleManagementService.revokeAllUserRoles(userId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/available")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RoleInfo>> getAvailableRoles() {
        List<RoleInfo> roles = roleManagementService.getAvailableRoles();
        return ResponseEntity.ok(roles);
    }

    @PutMapping("/update-expiration")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateRoleExpiration(
            @Valid @RequestBody UpdateRoleExpirationRequest request) {

        roleManagementService.updateRoleExpiration(request);

        return ResponseEntity.noContent().build();

    }

    @GetMapping("/check/{userId}/{roleName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Boolean> checkUserHasRole(
            @PathVariable UUID userId,
            @PathVariable String roleName) {

        boolean hasRole = roleManagementService.userHasRole(userId, roleName);
        return ResponseEntity.ok(hasRole);
    }
}
