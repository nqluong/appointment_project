package org.project.appointment_project.user.service.strategy;

import org.project.appointment_project.user.dto.request.ProfileUpdateRequest;
import org.project.appointment_project.user.dto.request.UpdateCompleteProfileRequest;

import java.util.Set;

public interface FieldFilterStrategy {
    /**
     * Filter request để chỉ giữ lại những field được phép theo role
     */
    ProfileUpdateRequest filterFields(ProfileUpdateRequest request);

    boolean supports(Set<String> roles);

    /**
     * Lấy danh sách fields được phép update cho user profile
     */
    Set<String> getAllowedUserProfileFields();

    /**
     * Lấy danh sách fields được phép update cho medical profile
     */
    Set<String> getAllowedMedicalProfileFields();
}
