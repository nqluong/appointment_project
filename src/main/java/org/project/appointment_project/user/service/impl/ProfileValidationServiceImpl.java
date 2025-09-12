package org.project.appointment_project.user.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.common.exception.CustomException;
import org.project.appointment_project.common.exception.ErrorCode;
import org.project.appointment_project.user.dto.request.UpdateCompleteProfileRequest;
import org.project.appointment_project.user.service.ProfileValidationService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class ProfileValidationServiceImpl implements ProfileValidationService {

    private static final int MAX_YEARS_EXPERIENCE = 70;
    private static final int MIN_CONSULTATION_FEE = 0;

    @Override
    public void validateProfileUpdateRequest(UpdateCompleteProfileRequest request, List<String> userRoles) {
        boolean hasDoctor = userRoles.contains("DOCTOR");
        boolean hasPatient = userRoles.contains("PATIENT");
        boolean hasAdmin = userRoles.contains("ADMIN");
        boolean hasOnlyAdmin = hasAdmin && !hasDoctor && !hasPatient;

        if (hasDoctor) {
            validateDoctorRequest(request);
        }

        if (hasPatient && !hasDoctor) {
            validatePatientOnlyRequest(request);
        }

        if (hasOnlyAdmin) {
            validateAdminOnlyRequest(request);
        }
    }

    /**
     * Validate request cho user chỉ có ADMIN role
     * Admin không được phép cập nhật bất kỳ medical fields nào
     */
    private void validateAdminOnlyRequest(UpdateCompleteProfileRequest request) {
        log.debug("Validating admin-only profile update request");

        if (hasMedicalFields(request) || hasDoctorFields(request)) {
            throw new CustomException(ErrorCode.INVALID_ROLE_OPERATION,
                    "Admins cannot update medical profile or doctor-specific fields");
        }
    }

    /**
     * Validate request cho user có DOCTOR role
     * Doctor phải có license number và các trường doctor khác hợp lệ
     */
    private void validateDoctorRequest(UpdateCompleteProfileRequest request) {
        log.debug("Validating doctor profile update request");

        // License number là bắt buộc cho doctor
        if (!StringUtils.hasText(request.getLicenseNumber())) {
            throw new CustomException(ErrorCode.MISSING_REQUIRED_FIELD,
                    "License number is required for doctors");
        }

        // Validate years of experience
        validateYearsOfExperience(request.getYearsOfExperience());

        // Validate consultation fee
        validateConsultationFee(request.getConsultationFee());
    }

    /**
     * Validate request cho user chỉ có PATIENT role
     * Patient không được phép cập nhật doctor-specific fields
     */
    private void validatePatientOnlyRequest(UpdateCompleteProfileRequest request) {
        log.debug("Validating patient-only profile update request");

        if (hasDoctorFields(request)) {
            throw new CustomException(ErrorCode.INVALID_ROLE_OPERATION,
                    "Patients cannot update doctor-specific fields");
        }
    }

    /**
     * Validate request cho user có nhiều role
     */
    private void validateMixedRolesRequest(UpdateCompleteProfileRequest request, Set<String> roles) {
        log.debug("Validating mixed roles profile update request for roles: {}", roles);

        if (roles.contains("DOCTOR")) {
            validateDoctorRequest(request);
        }
        // Nếu không có DOCTOR role nhưng có medical fields khác, validate như patient
        else if (roles.contains("PATIENT")) {
            validatePatientOnlyRequest(request);
        }
    }

    /**
     * Kiểm tra xem request có chứa medical fields không
     */
    private boolean hasMedicalFields(UpdateCompleteProfileRequest request) {
        return StringUtils.hasText(request.getBloodType()) ||
                StringUtils.hasText(request.getAllergies()) ||
                StringUtils.hasText(request.getMedicalHistory()) ||
                StringUtils.hasText(request.getEmergencyContactName()) ||
                StringUtils.hasText(request.getEmergencyContactPhone());
    }

    /**
     * Kiểm tra xem request có chứa doctor-specific fields không
     */
    private boolean hasDoctorFields(UpdateCompleteProfileRequest request) {
        return StringUtils.hasText(request.getLicenseNumber()) ||
                StringUtils.hasText(request.getQualification()) ||
                request.getYearsOfExperience() != null ||
                request.getConsultationFee() != null ||
                StringUtils.hasText(request.getBio()) ||
                StringUtils.hasText(request.getSpecialtyId());
    }

    /**
     * Validate years of experience
     */
    private void validateYearsOfExperience(Integer yearsOfExperience) {
        if (yearsOfExperience != null) {
            if (yearsOfExperience < 0) {
                throw new CustomException(ErrorCode.INVALID_INPUT,
                        "Years of experience cannot be negative");
            }
            if (yearsOfExperience > MAX_YEARS_EXPERIENCE) {
                throw new CustomException(ErrorCode.INVALID_INPUT,
                        "Years of experience seems unrealistic (max: " + MAX_YEARS_EXPERIENCE + ")");
            }
        }
    }

    /**
     * Validate consultation fee
     */
    private void validateConsultationFee(java.math.BigDecimal consultationFee) {
        if (consultationFee != null) {
            if (consultationFee.compareTo(java.math.BigDecimal.valueOf(MIN_CONSULTATION_FEE)) < 0) {
                throw new CustomException(ErrorCode.INVALID_INPUT,
                        "Consultation fee cannot be negative");
            }
        }
    }
}
