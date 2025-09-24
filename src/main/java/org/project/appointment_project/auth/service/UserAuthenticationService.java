package org.project.appointment_project.auth.service;


import org.project.appointment_project.user.model.User;

public interface UserAuthenticationService {
    User authenticateUser(String username, String password);
}
