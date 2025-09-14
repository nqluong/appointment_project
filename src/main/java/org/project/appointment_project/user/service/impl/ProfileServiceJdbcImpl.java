package org.project.appointment_project.user.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.common.exception.CustomException;
import org.project.appointment_project.common.exception.ErrorCode;
import org.project.appointment_project.common.security.annotation.RequireOwnershipOrAdmin;
import org.project.appointment_project.user.dto.request.ProfileUpdateRequest;
import org.project.appointment_project.user.dto.request.UpdateUserProfileRequest;
import org.project.appointment_project.user.dto.response.CompleteProfileResponse;
import org.project.appointment_project.user.mapper.ProfileMapper;
import org.project.appointment_project.user.model.User;
import org.project.appointment_project.user.repository.ProfileJdbcRepository;
import org.project.appointment_project.user.repository.UserRepository;
import org.project.appointment_project.user.service.ProfileService;
import org.project.appointment_project.user.service.ProfileValidationService;
import org.project.appointment_project.user.service.UserRoleService;
import org.project.appointment_project.user.service.strategy.FieldFilterStrategy;
import org.project.appointment_project.user.service.strategy.FieldFilterStrategyFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProfileServiceJdbcImpl implements ProfileService {
    ProfileJdbcRepository profileJdbcRepository;
    UserRepository userRepository;
    ProfileMapper profileMapper;
    ProfileValidationService profileValidationService;
    UserRoleService userRoleService;
    FieldFilterStrategyFactory fieldFilterStrategyFactory;

    @Override
    @Transactional
    @RequireOwnershipOrAdmin(allowedRoles = {"PATIENT", "DOCTOR", "ADMIN"})
    public CompleteProfileResponse updateProfile(UUID userId, ProfileUpdateRequest request) {
        try {
            log.info("Starting unified profile update for userId: {}", userId);

            // 1. Validate user exists
            User user = findUserByIdOrThrow(userId);

            // 2. Get user roles
            Set<String> userRoles = getUserRoles(userId);
            log.debug("User roles for filtering: {}", userRoles);

            // 3. Get appropriate strategy and filter fields
            FieldFilterStrategy strategy = fieldFilterStrategyFactory.getStrategy(userRoles);
            ProfileUpdateRequest filteredRequest = strategy.filterFields(request);

            log.debug("Request filtered by strategy: {}", strategy.getClass().getSimpleName());

            // 4. Update profile using filtered request
            boolean updateSuccess = profileJdbcRepository.updateProfile(userId, filteredRequest);

            if (!updateSuccess) {
                log.warn("No fields were updated for userId: {}", userId);
                // Still return current profile instead of throwing exception
            }

            // 5. Get and return complete profile after update
            CompleteProfileResponse response = getCompleteProfileFromRepository(userId);
            log.info("Unified profile update completed successfully for userId: {}", userId);
            return response;

        } catch (CustomException e) {
            log.error("Failed to update unified profile for userId: {} - {}", userId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error updating unified profile for userId: {}", userId, e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @RequireOwnershipOrAdmin(allowedRoles = {"PATIENT", "DOCTOR", "ADMIN"})
    public CompleteProfileResponse getCompleteProfile(UUID userId) {
        try {
            log.info("Getting complete profile for userId: {}", userId);

            // Validate user exists
            User user = findUserByIdOrThrow(userId);

            // Get complete profile from repository
            return getCompleteProfileFromRepository(userId);

        } catch (CustomException e) {
            log.error("Failed to retrieve complete profile for userId: {} - {}", userId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error retrieving complete profile for userId: {}", userId, e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Lấy complete profile từ repository và convert thành response
     */
    private CompleteProfileResponse getCompleteProfileFromRepository(UUID userId) {
        User completeUser = profileJdbcRepository.getCompleteUserProfile(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return profileMapper.toCompleteProfileResponse(completeUser);
    }

    /**
     * Helper methods từ parent implementation
     */
    private User findUserByIdOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private Set<String> getUserRoles(UUID userId) {
        return Set.copyOf(userRoleService.getUserRoleNames(userId));
    }
}
