package org.project.appointment_project.user.service.impl;


import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.common.exception.CustomException;
import org.project.appointment_project.common.exception.ErrorCode;
import org.project.appointment_project.common.security.annotation.RequireOwnershipOrAdmin;
import org.project.appointment_project.user.dto.request.UpdateCompleteProfileRequest;
import org.project.appointment_project.user.dto.request.UpdateMedicalProfileRequest;
import org.project.appointment_project.user.dto.request.UpdateUserProfileRequest;
import org.project.appointment_project.user.dto.response.CompleteProfileResponse;
import org.project.appointment_project.user.dto.response.UpdateMedicalProfileResponse;
import org.project.appointment_project.user.dto.response.UpdateUserProfileResponse;
import org.project.appointment_project.user.mapper.ProfileMapper;
import org.project.appointment_project.user.model.MedicalProfile;
import org.project.appointment_project.user.model.Specialty;
import org.project.appointment_project.user.model.User;
import org.project.appointment_project.user.model.UserProfile;
import org.project.appointment_project.user.repository.SpecialtyRepository;
import org.project.appointment_project.user.repository.UserRepository;
import org.project.appointment_project.user.service.ProfileService;
import org.project.appointment_project.user.service.ProfileValidationService;
import org.project.appointment_project.user.service.UserRoleService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProfileServiceImpl implements ProfileService {

    UserRepository userRepository;
    ProfileMapper profileMapper;
    ProfileValidationService profileValidationService;
    SpecialtyRepository specialtyRepository;
    UserRoleService userRoleService;

    @Override
    @Transactional
    @RequireOwnershipOrAdmin(allowedRoles = {"PATIENT", "DOCTOR"})
    public UpdateUserProfileResponse updateUserProfile(UUID userId, UpdateUserProfileRequest request) {
        try {
            User user = findUserById(userId);
            UserProfile userProfile = getOrCreateUserProfile(user, request);
            User savedUser = userRepository.save(user);
            return profileMapper.toUserProfileResponse(savedUser.getUserProfile());
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
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
            MedicalProfile medicalProfile = getOrCreateMedicalProfile(user, request);
            User savedUser = userRepository.save(user);

            return profileMapper.toMedicalProfileResponse(savedUser.getMedicalProfile());
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @Transactional
    @RequireOwnershipOrAdmin(allowedRoles = {"PATIENT", "DOCTOR", "ADMIN"})
    public CompleteProfileResponse updateCompleteProfile(UUID userId, UpdateCompleteProfileRequest request) {
        try {
            User user = findUserById(userId);
            List<String> userRoles = userRoleService.getUserRoleNames(userId);

            // Validate request based on user roles
            profileValidationService.validateProfileUpdateRequest(request, userRoles);

            boolean hasDoctor = userRoles.contains("DOCTOR");
            boolean hasPatient = userRoles.contains("PATIENT");
            boolean hasOnlyAdmin = userRoles.contains("ADMIN") && !hasDoctor && !hasPatient;

            // Update UserProfile (common for all roles except pure admin with medical fields)
            if (!hasOnlyAdmin) {
                updateUserProfileFromCompleteRequest(user, request);
            } else {
                updateUserProfileOnlyFromCompleteRequest(user, request);
            }

            // Update MedicalProfile based on roles
            if (hasDoctor || hasPatient) {
                updateMedicalProfileFromCompleteRequest(user, request, hasDoctor);
            }

            User savedUser = userRepository.save(user);
            return profileMapper.toCompleteProfileResponse(savedUser);

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @RequireOwnershipOrAdmin(allowedRoles = {"PATIENT", "DOCTOR", "ADMIN"})
    public CompleteProfileResponse getCompleteProfile(UUID userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

            return profileMapper.toCompleteProfileResponse(user);

        } catch (CustomException e) {
            log.error("Failed to retrieve complete profile for userId: {} - {}", userId, e.getMessage());
            throw e;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private User findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private UserProfile getOrCreateUserProfile(User user, UpdateUserProfileRequest request) {
        UserProfile userProfile = user.getUserProfile();
        if (userProfile == null) {
            userProfile = profileMapper.toUserProfileEntity(request);
            userProfile.setUser(user);
            user.setUserProfile(userProfile);
        } else {
            profileMapper.updateUserProfileEntity(userProfile, request);
        }
        return userProfile;
    }

    private MedicalProfile getOrCreateMedicalProfile(User user, UpdateMedicalProfileRequest request) {
        MedicalProfile medicalProfile = user.getMedicalProfile();
        if (medicalProfile == null) {
            medicalProfile = profileMapper.toMedicalProfileEntity(request);
            medicalProfile.setUser(user);
            user.setMedicalProfile(medicalProfile);
        } else {
            profileMapper.updateMedicalProfileEntity(medicalProfile, request);
        }
        return medicalProfile;
    }

    private void updateUserProfileFromCompleteRequest(User user, UpdateCompleteProfileRequest request) {
        UserProfile userProfile = user.getUserProfile();
        if (userProfile == null) {
            userProfile = profileMapper.toUserProfileFromCompleteRequest(request);
            userProfile.setUser(user);
            user.setUserProfile(userProfile);
        } else {
            profileMapper.updateUserProfileFromCompleteRequest(userProfile, request);
        }
    }

    private void updateUserProfileOnlyFromCompleteRequest(User user, UpdateCompleteProfileRequest request) {
        // For pure ADMIN role, only update basic user profile fields
        UserProfile userProfile = user.getUserProfile();
        if (userProfile == null) {
            userProfile = UserProfile.builder()
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .dateOfBirth(request.getDateOfBirth())
                    .gender(request.getGender())
                    .address(request.getAddress())
                    .phone(request.getPhone())
                    .avatarUrl(request.getAvatarUrl())
                    .user(user)
                    .build();
            user.setUserProfile(userProfile);
        } else {
            if (StringUtils.hasText(request.getFirstName())) userProfile.setFirstName(request.getFirstName());
            if (StringUtils.hasText(request.getLastName())) userProfile.setLastName(request.getLastName());
            if (request.getDateOfBirth() != null) userProfile.setDateOfBirth(request.getDateOfBirth());
            if (request.getGender() != null) userProfile.setGender(request.getGender());
            if (StringUtils.hasText(request.getAddress())) userProfile.setAddress(request.getAddress());
            if (StringUtils.hasText(request.getPhone())) userProfile.setPhone(request.getPhone());
            if (StringUtils.hasText(request.getAvatarUrl())) userProfile.setAvatarUrl(request.getAvatarUrl());
        }
    }

    private void updateMedicalProfileFromCompleteRequest(User user, UpdateCompleteProfileRequest request, boolean hasDoctor) {
        MedicalProfile medicalProfile = user.getMedicalProfile();
        if (medicalProfile == null) {
            medicalProfile = profileMapper.toMedicalProfileFromCompleteRequest(request);
            medicalProfile.setUser(user);
            user.setMedicalProfile(medicalProfile);
        } else {
            profileMapper.updateMedicalProfileFromCompleteRequest(medicalProfile, request);
        }

        // Set specialty for doctors only
        if (hasDoctor && StringUtils.hasText(request.getSpecialtyId())) {
            try {
                UUID specialtyId = UUID.fromString(request.getSpecialtyId());
                Specialty specialty = specialtyRepository.findById(specialtyId)
                        .orElseThrow(() -> new CustomException(ErrorCode.SPECIALTY_NOT_FOUND));
                medicalProfile.setSpecialty(specialty);
            } catch (IllegalArgumentException e) {
                throw new CustomException(ErrorCode.INVALID_UUID_FORMAT, "Invalid specialty ID format");
            }
        }
    }
}
