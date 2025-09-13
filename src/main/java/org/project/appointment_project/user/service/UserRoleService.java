package org.project.appointment_project.user.service;

import java.util.List;
import java.util.UUID;

public interface UserRoleService {
    List<String> getUserRoleNames(UUID userId);
    boolean hasRole(UUID userId, String roleName);
    boolean hasActiveRole(UUID userId, UUID roleId);
}
