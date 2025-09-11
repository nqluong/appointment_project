package org.project.appointment_project.user.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.project.appointment_project.common.exception.CustomException;
import org.project.appointment_project.common.exception.ErrorCode;
import org.project.appointment_project.user.dto.request.DoctorRegistrationRequest;
import org.project.appointment_project.user.dto.request.PatientRegistrationRequest;

import org.project.appointment_project.user.repository.MedicalProfileRepository;
import org.project.appointment_project.user.repository.UserProfileRepository;
import org.project.appointment_project.user.repository.UserRepository;
import org.project.appointment_project.user.service.UserRegistrationValidator;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserRegistrationValidatorImpl implements UserRegistrationValidator {
    UserRepository userRepository;
    UserProfileRepository userProfileRepository;
    MedicalProfileRepository medicalProfileRepository;

    @Override
    public void validatePatientRegistration(PatientRegistrationRequest request) {
        validateCommonFields(request.getUsername(), request.getEmail());
    }

    @Override
    public void validateDoctorRegistration(DoctorRegistrationRequest request) {
        validateCommonFields(request.getUsername(), request.getEmail());
        validateLicenseNumber(request.getLicenseNumber());
    }

    private void validateCommonFields(String username, String email) {
        if(userRepository.existsByUsername(username)) {
            throw new CustomException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }
        if(userRepository.existsByEmail(email)) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
    }

    private void validateLicenseNumber(String licenseNumber) {
        if(medicalProfileRepository.existsByLicenseNumber(licenseNumber)) {
            throw new CustomException(ErrorCode.LICENSE_NUMBER_ALREADY_EXISTS);
        }
    }
}
