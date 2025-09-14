package org.project.appointment_project.user.repository;

import org.project.appointment_project.user.dto.request.ProfileUpdateRequest;
import org.project.appointment_project.user.dto.request.UpdateCompleteProfileRequest;
import org.project.appointment_project.user.dto.request.UpdateMedicalProfileRequest;
import org.project.appointment_project.user.dto.request.UpdateUserProfileRequest;
import org.project.appointment_project.user.model.MedicalProfile;
import org.project.appointment_project.user.model.User;
import org.project.appointment_project.user.model.UserProfile;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ProfileJdbcRepository {

    /**
     * Cập nhật profile thông qua request (đã được filter bởi strategy)
     * @param userId ID của user
     * @param request update request (đã được filter)
     * @return true nếu cập nhật thành công
     */
    boolean updateProfile(UUID userId, ProfileUpdateRequest request);

    /**
     * Lấy complete profile information
     * @param userId ID của user
     * @return User với đầy đủ profile information
     */
    Optional<User> getCompleteUserProfile(UUID userId);

    /**
     * Tạo mới UserProfile bằng JDBC
     * @param userId ID của user
     * @param request Request để tạo user profile
     * @return ID của UserProfile được tạo
     */
    UUID createUserProfile(UUID userId, UpdateUserProfileRequest request);

    /**
     * Tạo mới MedicalProfile bằng JDBC
     * @param userId ID của user
     * @param request Request để tạo medical profile
     * @return ID của MedicalProfile được tạo
     */
    UUID createMedicalProfile(UUID userId, UpdateMedicalProfileRequest request);

    /**
     * Lấy thông tin UserProfile theo userId
     * @param userId ID của user
     * @return Optional UserProfile
     */
    Optional<UserProfile> findUserProfileByUserId(UUID userId);

    /**
     * Lấy thông tin MedicalProfile theo userId
     * @param userId ID của user
     * @return Optional MedicalProfile
     */
    Optional<MedicalProfile> findMedicalProfileByUserId(UUID userId);

    /**
     * Kiểm tra UserProfile có tồn tại không
     * @param userId ID của user
     * @return true nếu tồn tại
     */
    boolean existsUserProfile(UUID userId);

    /**
     * Kiểm tra MedicalProfile có tồn tại không
     * @param userId ID của user
     * @return true nếu tồn tại
     */
    boolean existsMedicalProfile(UUID userId);
}
