package org.project.appointment_project.user.service.strategy.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.user.dto.request.UpdateCompleteProfileRequest;
import org.project.appointment_project.user.service.strategy.FieldFilterStrategy;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DoctorFieldFilterStrategy implements FieldFilterStrategy {

    @Override
    public UpdateCompleteProfileRequest filterFields(UpdateCompleteProfileRequest request) {
        return UpdateCompleteProfileRequest.builder()

                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .address(request.getAddress())
                .phone(request.getPhone())
                .avatarUrl(request.getAvatarUrl())

                .bloodType(request.getBloodType())
                .allergies(request.getAllergies())
                .medicalHistory(request.getMedicalHistory())
                .emergencyContactName(request.getEmergencyContactName())
                .emergencyContactPhone(request.getEmergencyContactPhone())

                .licenseNumber(request.getLicenseNumber())
                .qualification(request.getQualification())
                .yearsOfExperience(request.getYearsOfExperience())
                .consultationFee(request.getConsultationFee())
                .bio(request.getBio())
                .specialtyId(request.getSpecialtyId())
                .build();
    }

    @Override
    public boolean supports(Set<String> roles) {
       return roles.contains("DOCTOR");
    }
}
