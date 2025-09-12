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
import org.project.appointment_project.user.service.strategy.FieldFilterStrategy;
import org.project.appointment_project.user.service.strategy.FieldFilterStrategyFactory;
import org.project.appointment_project.user.service.strategy.ProfileUpdateStrategy;
import org.project.appointment_project.user.service.strategy.ProfileUpdateStrategyFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProfileServiceImpl implements ProfileService {

    UserRepository userRepository;
    ProfileMapper profileMapper;
    ProfileValidationService profileValidationService;
    UserRoleService userRoleService;
    ProfileUpdateStrategyFactory profileUpdateStrategyFactory;
    FieldFilterStrategyFactory fieldFilterStrategyFactory;

    @Override
    @Transactional
    @RequireOwnershipOrAdmin(allowedRoles = {"PATIENT", "DOCTOR"})
    public UpdateUserProfileResponse updateUserProfile(UUID userId, UpdateUserProfileRequest request) {
        try {
            User user = findUserByIdOrThrow(userId);

            profileMapper.createOrUpdateUserProfile(user, request);

            User savedUser = userRepository.save(user);
            return profileMapper.toUserProfileResponse(savedUser.getUserProfile());

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
        try {
            User user = findUserByIdOrThrow(userId);

            profileMapper.createOrUpdateMedicalProfile(user, request);

            User savedUser = userRepository.save(user);
            return profileMapper.toMedicalProfileResponse(savedUser.getMedicalProfile());

        } catch (CustomException e) {
            log.error("Failed to update medical profile for userId: {} - {}", userId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error updating medical profile for userId: {}", userId, e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @Transactional
    @RequireOwnershipOrAdmin(allowedRoles = {"PATIENT", "DOCTOR", "ADMIN"})
    public CompleteProfileResponse updateCompleteProfile(UUID userId, UpdateCompleteProfileRequest request) {
        try {
            User user = findUserByIdOrThrow(userId);
            Set<String> userRoles = getUserRoles(userId);

            //Lọc field theo role của user
            FieldFilterStrategy fieldFilterStrategy = fieldFilterStrategyFactory.getStrategy(userRoles);
            UpdateCompleteProfileRequest requestFilter = fieldFilterStrategy.filterFields(request);

            // Validate request dựa trên quyền của user
            profileValidationService.validateProfileUpdateRequest(requestFilter, userRoles.stream().toList());

            // Sử dụng Strategy pattern để xử lý update dựa trên role
            ProfileUpdateStrategy updateStrategy = profileUpdateStrategyFactory.getStrategy(userRoles);
            updateStrategy.updateProfile(user, requestFilter);

            User savedUser = userRepository.save(user);
            return profileMapper.toCompleteProfileResponse(savedUser);

        } catch (CustomException e) {
            log.error("Failed to update complete profile for userId: {} - {}", userId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error updating complete profile for userId: {}", userId, e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @RequireOwnershipOrAdmin(allowedRoles = {"PATIENT", "DOCTOR", "ADMIN"})
    public CompleteProfileResponse getCompleteProfile(UUID userId) {
        try {
            User user = findUserByIdOrThrow(userId);
            return profileMapper.toCompleteProfileResponse(user);

        } catch (CustomException e) {
            log.error("Failed to retrieve complete profile for userId: {} - {}", userId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error retrieving complete profile for userId: {}", userId, e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private User findUserByIdOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private Set<String> getUserRoles(UUID userId) {
        return Set.copyOf(userRoleService.getUserRoleNames(userId));
    }

}
