package org.project.appointment_project.user.mapper;

import org.mapstruct.*;
import org.project.appointment_project.user.dto.request.UpdateCompleteProfileRequest;
import org.project.appointment_project.user.dto.request.UpdateMedicalProfileRequest;
import org.project.appointment_project.user.dto.request.UpdateUserProfileRequest;
import org.project.appointment_project.user.dto.response.CompleteProfileResponse;
import org.project.appointment_project.user.dto.response.UpdateMedicalProfileResponse;
import org.project.appointment_project.user.dto.response.UpdateUserProfileResponse;
import org.project.appointment_project.user.model.MedicalProfile;
import org.project.appointment_project.user.model.User;
import org.project.appointment_project.user.model.UserProfile;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface ProfileMapper {
    /**
     * Tạo UserProfile entity từ request, bỏ qua các trường hệ thống
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    UserProfile toUserProfileEntity(UpdateUserProfileRequest request);

    /**
     * Cập nhật UserProfile entity từ request, chỉ update các trường không null
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDateTime.now())")
    void updateUserProfileEntity(@MappingTarget UserProfile target, UpdateUserProfileRequest source);

    /**
     * Chuyển đổi UserProfile entity thành response DTO
     */
    UpdateUserProfileResponse toUserProfileResponse(UserProfile userProfile);

    /**
     * Tạo hoặc cập nhật UserProfile cho User entity
     * Nếu user chưa có UserProfile thì tạo mới, ngược lại thì cập nhật
     */
    @AfterMapping
    default void createOrUpdateUserProfile(@MappingTarget User user, UpdateUserProfileRequest request) {
        if (user.getUserProfile() == null) {
            UserProfile userProfile = toUserProfileEntity(request);
            userProfile.setUser(user);
            user.setUserProfile(userProfile);
        } else {
            updateUserProfileEntity(user.getUserProfile(), request);
        }
    }


    /**
     * Tạo MedicalProfile entity từ request, bỏ qua các field hệ thống
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "specialty", ignore = true)
    @Mapping(target = "isDoctorApproved", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    MedicalProfile toMedicalProfileEntity(UpdateMedicalProfileRequest request);

    /**
     * Cập nhật MedicalProfile entity từ request
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "specialty", ignore = true)
//    @Mapping(target = "isDoctorApproved", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDateTime.now())")
    void updateMedicalProfileEntity(@MappingTarget MedicalProfile target, UpdateMedicalProfileRequest source);

    /**
     * Chuyển đổi MedicalProfile entity thành response DTO
     */
    UpdateMedicalProfileResponse toMedicalProfileResponse(MedicalProfile medicalProfile);

    /**
     * Tạo hoặc cập nhật MedicalProfile cho User entity
     */
    @AfterMapping
    default void createOrUpdateMedicalProfile(@MappingTarget User user, UpdateMedicalProfileRequest request) {
        if (user.getMedicalProfile() == null) {
            MedicalProfile medicalProfile = toMedicalProfileEntity(request);
            medicalProfile.setUser(user);
            user.setMedicalProfile(medicalProfile);
        } else {
            updateMedicalProfileEntity(user.getMedicalProfile(), request);
        }
    }



    /**
     * Tạo UserProfile entity từ complete request (chỉ các field basic)
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "user", ignore = true)
    UserProfile toUserProfileFromCompleteRequest(UpdateCompleteProfileRequest request);

    /**
     * Tạo MedicalProfile entity từ complete request
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "specialty", ignore = true)
    @Mapping(target = "isDoctorApproved", ignore = true)
    MedicalProfile toMedicalProfileFromCompleteRequest(UpdateCompleteProfileRequest request);

    /**
     * Cập nhật UserProfile từ complete request
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDateTime.now())")
    void updateUserProfileFromCompleteRequest(@MappingTarget UserProfile userProfile, UpdateCompleteProfileRequest request);

    /**
     * Cập nhật MedicalProfile từ complete request
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "specialty", ignore = true)
//    @Mapping(target = "isDoctorApproved", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDateTime.now())")
    void updateMedicalProfileFromCompleteRequest(@MappingTarget MedicalProfile medicalProfile, UpdateCompleteProfileRequest request);

    /**
     * Tạo complete profile response từ User entity
     * Map tất cả thông tin từ UserProfile và MedicalProfile
     */
    @Mapping(source = "userProfile.id", target = "userProfileId")
    @Mapping(source = "userProfile.firstName", target = "firstName")
    @Mapping(source = "userProfile.lastName", target = "lastName")
    @Mapping(source = "userProfile.dateOfBirth", target = "dateOfBirth")
    @Mapping(source = "userProfile.gender", target = "gender")
    @Mapping(source = "userProfile.address", target = "address")
    @Mapping(source = "userProfile.phone", target = "phone")
    @Mapping(source = "userProfile.avatarUrl", target = "avatarUrl")
    @Mapping(source = "userProfile.updatedAt", target = "userProfileUpdatedAt")

    @Mapping(source = "medicalProfile.id", target = "medicalProfileId")
    @Mapping(source = "medicalProfile.bloodType", target = "bloodType")
    @Mapping(source = "medicalProfile.allergies", target = "allergies")
    @Mapping(source = "medicalProfile.medicalHistory", target = "medicalHistory")
    @Mapping(source = "medicalProfile.emergencyContactName", target = "emergencyContactName")
    @Mapping(source = "medicalProfile.emergencyContactPhone", target = "emergencyContactPhone")
    @Mapping(source = "medicalProfile.licenseNumber", target = "licenseNumber")
    @Mapping(source = "medicalProfile.specialty.name", target = "specialtyName")
    @Mapping(source = "medicalProfile.qualification", target = "qualification")
    @Mapping(source = "medicalProfile.yearsOfExperience", target = "yearsOfExperience")
    @Mapping(source = "medicalProfile.consultationFee", target = "consultationFee")
    @Mapping(source = "medicalProfile.bio", target = "bio")
//    @Mapping(source = "medicalProfile.isDoctorApproved", target = "isDoctorApproved")
    @Mapping(source = "medicalProfile.updatedAt", target = "medicalProfileUpdatedAt")
    CompleteProfileResponse toCompleteProfileResponse(User user);


    /**
     * Tạo UserProfile cơ bản chỉ với các trường admin được phép (không có medical fields)
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDateTime.now())")
    UserProfile createBasicUserProfile(
            String firstName, String lastName,
            @Context java.time.LocalDate dateOfBirth,
            @Context org.project.appointment_project.user.enums.Gender gender,
            String address, String phone, String avatarUrl
    );
}
