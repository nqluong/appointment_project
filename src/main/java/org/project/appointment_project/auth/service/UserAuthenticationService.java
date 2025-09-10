package org.project.appointment_project.auth.service;


import org.project.appointment_project.user.model.User;

import java.util.List;
import java.util.UUID;

public interface UserAuthenticationService {
    User authenticateUser(String username, String password);
    List<String> getUserRoles(UUID userId);
}
