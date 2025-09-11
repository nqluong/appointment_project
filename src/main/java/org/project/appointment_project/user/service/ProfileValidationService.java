package org.project.appointment_project.user.service;

import org.project.appointment_project.user.dto.request.UpdateCompleteProfileRequest;

import java.util.List;

public interface ProfileValidationService {
    void validateProfileUpdateRequest(UpdateCompleteProfileRequest request, List<String> userRoles);
}
