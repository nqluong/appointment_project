package org.project.appointment_project.user.service;

import org.project.appointment_project.user.dto.request.UpdateCompleteProfileRequest;
import org.project.appointment_project.user.dto.request.UpdateMedicalProfileRequest;
import org.project.appointment_project.user.dto.request.UpdateUserProfileRequest;
import org.project.appointment_project.user.dto.response.CompleteProfileResponse;
import org.project.appointment_project.user.dto.response.UpdateMedicalProfileResponse;
import org.project.appointment_project.user.dto.response.UpdateUserProfileResponse;

import java.util.UUID;

public interface ProfileService {
    UpdateUserProfileResponse updateUserProfile(UUID userId, UpdateUserProfileRequest request);
    UpdateMedicalProfileResponse updateMedicalProfile(UUID userId, UpdateMedicalProfileRequest request);

    CompleteProfileResponse updateCompleteProfile(UUID userId, UpdateCompleteProfileRequest request);
    CompleteProfileResponse getCompleteProfile(UUID userId);
}
