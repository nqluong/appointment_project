package org.project.appointment_project.auth.service;

import org.project.appointment_project.auth.dto.request.LoginRequest;
import org.project.appointment_project.auth.dto.response.LoginResponse;

public interface AuthenticationManager {
    LoginResponse authenticate(LoginRequest loginRequest);
}
