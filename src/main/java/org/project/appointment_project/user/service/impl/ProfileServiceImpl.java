package org.project.appointment_project.user.service.impl;


import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.common.exception.CustomException;
import org.project.appointment_project.common.exception.ErrorCode;
import org.project.appointment_project.common.security.annotation.RequireOwnershipOrAdmin;
import org.project.appointment_project.user.dto.request.UpdateMedicalProfileRequest;
import org.project.appointment_project.user.dto.request.UpdateUserProfileRequest;
import org.project.appointment_project.user.dto.response.UpdateMedicalProfileResponse;
import org.project.appointment_project.user.dto.response.UpdateUserProfileResponse;
import org.project.appointment_project.user.mapper.ProfileMapper;
import org.project.appointment_project.user.model.MedicalProfile;
import org.project.appointment_project.user.model.User;
import org.project.appointment_project.user.model.UserProfile;
import org.project.appointment_project.user.repository.UserRepository;
import org.project.appointment_project.user.service.ProfileService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProfileServiceImpl implements ProfileService {

    UserRepository userRepository;
    ProfileMapper profileMapper;

    @Override
    @Transactional
    @RequireOwnershipOrAdmin(allowedRoles = {"PATIENT", "DOCTOR"})
    public UpdateUserProfileResponse updateUserProfile(UUID userId, UpdateUserProfileRequest request) {
        try {
            User user = findUserById(userId);

            UserProfile userProfile = user.getUserProfile();
            if (userProfile == null) {
                userProfile = profileMapper.toUserProfileEntity(request);
                userProfile.setUser(user);
                user.setUserProfile(userProfile);
            } else {
                profileMapper.updateUserProfileEntity(userProfile, request);
            }

            User savedUser = userRepository.save(user);
            UpdateUserProfileResponse response = profileMapper.toUserProfileResponse(savedUser.getUserProfile());

            return response;

        } catch (CustomException e) {
            log.error("Failed to update user profile for userId: {} - {}", userId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error updating user profile for userId: {}", userId, e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @Transactional
    @RequireOwnershipOrAdmin(allowedRoles = {"PATIENT", "DOCTOR"})
    public UpdateMedicalProfileResponse updateMedicalProfile(UUID userId, UpdateMedicalProfileRequest request) {
        log.info("Starting medical profile update for userId: {}", userId);

        try {
            User user = findUserById(userId);

            MedicalProfile medicalProfile = user.getMedicalProfile();
            if (medicalProfile == null) {
                medicalProfile = profileMapper.toMedicalProfileEntity(request);
                medicalProfile.setUser(user);
                user.setMedicalProfile(medicalProfile);
                log.info("Created new medical profile for userId: {}", userId);
            } else {
                profileMapper.updateMedicalProfileEntity(medicalProfile, request);
                log.info("Updated existing medical profile for userId: {}", userId);
            }

            User savedUser = userRepository.save(user);
            UpdateMedicalProfileResponse response = profileMapper.toMedicalProfileResponse(savedUser.getMedicalProfile());

            log.info("Successfully updated medical profile for userId: {}", userId);
            return response;

        } catch (CustomException e) {
            log.error("Failed to update medical profile for userId: {} - {}", userId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error updating medical profile for userId: {}", userId, e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private User findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
