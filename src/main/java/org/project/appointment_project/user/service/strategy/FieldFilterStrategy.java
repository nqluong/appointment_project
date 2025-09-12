package org.project.appointment_project.user.service.strategy;

import org.project.appointment_project.user.dto.request.UpdateCompleteProfileRequest;

import java.util.Set;

public interface FieldFilterStrategy {
    /**
     * Filter request để chỉ giữ lại những field được phép theo role
     */
    UpdateCompleteProfileRequest filterFields(UpdateCompleteProfileRequest request);

    boolean supports(Set<String> roles);
}
