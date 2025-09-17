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
    @Mapping(source = "medicalProfile.updatedAt", target = "medicalProfileUpdatedAt")
    CompleteProfileResponse toCompleteProfileResponse(User user);

}
