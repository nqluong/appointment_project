package org.project.appointment_project.user.service.strategy.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.user.dto.request.UpdateCompleteProfileRequest;
import org.project.appointment_project.user.mapper.ProfileMapper;
import org.project.appointment_project.user.model.User;
import org.project.appointment_project.user.service.strategy.ProfileUpdateStrategy;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PatientOnlyProfileUpdateStrategy implements ProfileUpdateStrategy {

    ProfileMapper profileMapper;

    @Override
    public void updateProfile(User user, UpdateCompleteProfileRequest request) {
        // Update UserProfile
        updateUserProfile(user, request);

        // Update MedicalProfile (không có doctor-specific fields)
        updateMedicalProfile(user, request);
    }

    @Override
    public boolean supports(Set<String> roles) {
        return roles.contains("PATIENT") && !roles.contains("DOCTOR");
    }

    private void updateUserProfile(User user, UpdateCompleteProfileRequest request) {
        if (user.getUserProfile() == null) {
            var userProfile = profileMapper.toUserProfileFromCompleteRequest(request);
            userProfile.setUser(user);
            user.setUserProfile(userProfile);
        } else {
            profileMapper.updateUserProfileFromCompleteRequest(user.getUserProfile(), request);
        }
    }

    /**
     * Cập nhật MedicalProfile cho patient (không có specialty)
     */
    private void updateMedicalProfile(User user, UpdateCompleteProfileRequest request) {
        if (user.getMedicalProfile() == null) {
            var medicalProfile = profileMapper.toMedicalProfileFromCompleteRequest(request);
            medicalProfile.setUser(user);
            user.setMedicalProfile(medicalProfile);
        } else {
            profileMapper.updateMedicalProfileFromCompleteRequest(user.getMedicalProfile(), request);
        }
    }
}
