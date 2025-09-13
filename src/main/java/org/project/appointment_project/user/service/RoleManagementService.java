package org.project.appointment_project.user.service;

import org.project.appointment_project.user.dto.request.AssignRoleRequest;
import org.project.appointment_project.user.dto.request.UpdateRoleExpirationRequest;
import org.project.appointment_project.user.dto.response.RoleInfo;

import java.util.List;
import java.util.UUID;

public interface RoleManagementService {
    List<String> getUserRoles(UUID userId);

    void assignRoleToUser(AssignRoleRequest request, UUID assignedBy);

    void revokeRoleFromUser(UUID userId, UUID roleId);

    void revokeAllUserRoles(UUID userId);

    List<RoleInfo> getAvailableRoles();

    void updateRoleExpiration(UpdateRoleExpirationRequest request);

    boolean userHasRole(UUID userId, String roleName);
}
