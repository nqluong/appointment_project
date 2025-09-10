package org.project.appointment_project.auth.service;

import org.project.appointment_project.auth.dto.request.LogoutRequest;

public interface SessionManager {
    void terminateSession(LogoutRequest request);
}
