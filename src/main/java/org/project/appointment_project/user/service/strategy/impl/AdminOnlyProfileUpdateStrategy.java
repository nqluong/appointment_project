package org.project.appointment_project.user.service.strategy.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.user.dto.request.UpdateCompleteProfileRequest;
import org.project.appointment_project.user.mapper.ProfileMapper;
import org.project.appointment_project.user.model.User;
import org.project.appointment_project.user.model.UserProfile;
import org.project.appointment_project.user.service.strategy.ProfileUpdateStrategy;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminOnlyProfileUpdateStrategy implements ProfileUpdateStrategy {

    ProfileMapper profileMapper;

    @Override
    public void updateProfile(User user, UpdateCompleteProfileRequest request) {
        updateBasicUserProfile(user, request);
    }

    @Override
    public boolean supports(Set<String> roles) {
        return roles.contains("ADMIN") &&
                !roles.contains("DOCTOR") &&
                !roles.contains("PATIENT");
    }

    private void updateBasicUserProfile(User user, UpdateCompleteProfileRequest request) {
        UserProfile userProfile = user.getUserProfile();

        if (userProfile == null) {
            // Tạo mới UserProfile với chỉ các trường cơ bản
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
            // Cập nhật các trường được phép
            updateFieldIfNotNull(request.getFirstName(), userProfile::setFirstName);
            updateFieldIfNotNull(request.getLastName(), userProfile::setLastName);
            updateFieldIfNotNull(request.getDateOfBirth(), userProfile::setDateOfBirth);
            updateFieldIfNotNull(request.getGender(), userProfile::setGender);
            updateFieldIfNotNull(request.getAddress(), userProfile::setAddress);
            updateFieldIfNotNull(request.getPhone(), userProfile::setPhone);
            updateFieldIfNotNull(request.getAvatarUrl(), userProfile::setAvatarUrl);
        }
    }

    /**
     * Helper method để update field chỉ khi value không null/empty
     */
    private <T> void updateFieldIfNotNull(T value, Consumer<T> setter) {
        if (value != null && (!(value instanceof String) || StringUtils.hasText((String) value))) {
            setter.accept(value);
        }
    }
}
