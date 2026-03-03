package org.project.appointment_project.user.service;

import java.util.UUID;

import org.project.appointment_project.user.dto.request.ProfileUpdateRequest;
import org.project.appointment_project.user.dto.response.CompleteProfileResponse;

public interface ProfileService {

    CompleteProfileResponse getCompleteProfile(UUID userId);

    CompleteProfileResponse getCompleteProfileInternal(UUID userId);

    CompleteProfileResponse updateProfile(UUID userId, ProfileUpdateRequest request);

    CompleteProfileResponse updateProfileInternal(UUID userId, ProfileUpdateRequest request);
}
