package org.project.appointment_project.user.service.strategy;

import org.project.appointment_project.user.dto.request.UpdateCompleteProfileRequest;
import org.project.appointment_project.user.model.User;

import java.util.Set;

public interface ProfileUpdateStrategy {
    /**
     * Cập nhật profile của user dựa trên request và role
     */
    void updateProfile(User user, UpdateCompleteProfileRequest request);


    boolean supports(Set<String> roles);
}
