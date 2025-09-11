package org.project.appointment_project.user.service.strategy.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.common.exception.CustomException;
import org.project.appointment_project.common.exception.ErrorCode;
import org.project.appointment_project.user.dto.request.UpdateCompleteProfileRequest;
import org.project.appointment_project.user.mapper.ProfileMapper;
import org.project.appointment_project.user.model.MedicalProfile;
import org.project.appointment_project.user.model.Specialty;
import org.project.appointment_project.user.model.User;
import org.project.appointment_project.user.repository.SpecialtyRepository;
import org.project.appointment_project.user.service.strategy.ProfileUpdateStrategy;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DoctorProfileUpdateStrategy implements ProfileUpdateStrategy {
    ProfileMapper profileMapper;
    SpecialtyRepository specialtyRepository;

    @Override
    public void updateProfile(User user, UpdateCompleteProfileRequest request) {
        // Update UserProfile
        updateUserProfile(user, request);

        // Update MedicalProfile (medical + doctor specific info)
        updateMedicalProfile(user, request);
    }

    @Override
    public boolean supports(Set<String> roles) {
        return roles.contains("DOCTOR");
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
     * Cập nhật MedicalProfile bao gồm specialty (nếu có) cho doctor
     */
    private void updateMedicalProfile(User user, UpdateCompleteProfileRequest request) {
        if (user.getMedicalProfile() == null) {
            var medicalProfile = profileMapper.toMedicalProfileFromCompleteRequest(request);
            medicalProfile.setUser(user);
            user.setMedicalProfile(medicalProfile);
        } else {
            profileMapper.updateMedicalProfileFromCompleteRequest(user.getMedicalProfile(), request);
        }

        // Set specialty nếu có
        setSpecialtyIfProvided(user.getMedicalProfile(), request.getSpecialtyId());
    }

    /**
     * Set specialty cho doctor nếu specialtyId được cung cấp
     */
    private void setSpecialtyIfProvided(MedicalProfile medicalProfile, String specialtyId) {
        if (StringUtils.hasText(specialtyId)) {
            try {
                UUID specialtyUuid = UUID.fromString(specialtyId);
                Specialty specialty = specialtyRepository.findById(specialtyUuid)
                        .orElseThrow(() -> new CustomException(ErrorCode.SPECIALTY_NOT_FOUND));
                medicalProfile.setSpecialty(specialty);
            } catch (IllegalArgumentException e) {
                throw new CustomException(ErrorCode.INVALID_UUID_FORMAT, "Invalid specialty ID format");
            }
        }
    }
}
