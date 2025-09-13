package org.project.appointment_project.auth.service;


import org.project.appointment_project.user.dto.request.AssignRoleRequest;
import org.project.appointment_project.user.dto.request.UpdateRoleExpirationRequest;
import org.project.appointment_project.user.dto.response.RoleInfo;
import org.project.appointment_project.user.dto.response.UserRoleDetail;
import org.project.appointment_project.user.model.User;

import java.util.List;
import java.util.UUID;

public interface UserAuthenticationService {
    User authenticateUser(String username, String password);
}
