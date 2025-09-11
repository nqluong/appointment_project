package org.project.appointment_project.user.mapper;

import org.mapstruct.*;
import org.project.appointment_project.user.dto.request.UpdateMedicalProfileRequest;
import org.project.appointment_project.user.dto.request.UpdateUserProfileRequest;
import org.project.appointment_project.user.dto.response.UpdateMedicalProfileResponse;
import org.project.appointment_project.user.dto.response.UpdateUserProfileResponse;
import org.project.appointment_project.user.model.MedicalProfile;
import org.project.appointment_project.user.model.UserProfile;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface ProfileMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    UserProfile toUserProfileEntity(UpdateUserProfileRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDateTime.now())")
    void updateUserProfileEntity(@MappingTarget UserProfile target, UpdateUserProfileRequest source);

    UpdateUserProfileResponse toUserProfileResponse(UserProfile userProfile);

    // Medical Profile Mappings
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    MedicalProfile toMedicalProfileEntity(UpdateMedicalProfileRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", expression = "java(java.time.LocalDateTime.now())")
    void updateMedicalProfileEntity(@MappingTarget MedicalProfile target, UpdateMedicalProfileRequest source);

    UpdateMedicalProfileResponse toMedicalProfileResponse(MedicalProfile medicalProfile);
}
