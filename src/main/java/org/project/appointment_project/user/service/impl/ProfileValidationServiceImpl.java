package org.project.appointment_project.user.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.common.exception.CustomException;
import org.project.appointment_project.common.exception.ErrorCode;
import org.project.appointment_project.user.dto.request.UpdateCompleteProfileRequest;
import org.project.appointment_project.user.service.ProfileValidationService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@Slf4j
public class ProfileValidationServiceImpl implements ProfileValidationService {
    @Override
    public void validateProfileUpdateRequest(UpdateCompleteProfileRequest request, List<String> userRoles) {
        boolean hasDoctor = userRoles.contains("DOCTOR");
        boolean hasPatient = userRoles.contains("PATIENT");
        boolean hasAdmin = userRoles.contains("ADMIN");
        boolean hasOnlyAdmin = hasAdmin && !hasDoctor && !hasPatient;

        if (hasDoctor) {
            validateDoctorFields(request);
        }

        if (hasPatient && !hasDoctor) {
            validatePatientFields(request);
        }

        if (hasOnlyAdmin) {
            validateAdminFields(request);
        }
    }

    private void validateAdminFields(UpdateCompleteProfileRequest request) {
        if (StringUtils.hasText(request.getBloodType()) ||
                StringUtils.hasText(request.getAllergies()) ||
                StringUtils.hasText(request.getMedicalHistory()) ||
                StringUtils.hasText(request.getEmergencyContactName()) ||
                StringUtils.hasText(request.getEmergencyContactPhone()) ||
                StringUtils.hasText(request.getLicenseNumber()) ||
                StringUtils.hasText(request.getQualification()) ||
                request.getYearsOfExperience() != null ||
                request.getConsultationFee() != null ||
                StringUtils.hasText(request.getBio()) ||
                StringUtils.hasText(request.getSpecialtyId())) {

            throw new CustomException(ErrorCode.INVALID_ROLE_OPERATION,
                    "Admins cannot update medical profile fields");
        }
    }

    private void validateDoctorFields(UpdateCompleteProfileRequest request) {
        if (!StringUtils.hasText(request.getLicenseNumber())) {
            throw new CustomException(ErrorCode.MISSING_REQUIRED_FIELD, "License number is required for doctors");
        }

        if (request.getYearsOfExperience() != null && request.getYearsOfExperience() > 70) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Years of experience seems unrealistic");
        }

        if (request.getConsultationFee() != null && request.getConsultationFee().intValue() < 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "Consultation fee seems unrealistic");
        }
    }

    private void validatePatientFields(UpdateCompleteProfileRequest request) {
        if (StringUtils.hasText(request.getLicenseNumber()) ||
                StringUtils.hasText(request.getQualification()) ||
                request.getYearsOfExperience() != null ||
                request.getConsultationFee() != null ||
                StringUtils.hasText(request.getBio()) ||
                StringUtils.hasText(request.getSpecialtyId())) {

            throw new CustomException(ErrorCode.INVALID_ROLE_OPERATION,
                    "Patients cannot update doctor-specific fields");
        }
    }

}
