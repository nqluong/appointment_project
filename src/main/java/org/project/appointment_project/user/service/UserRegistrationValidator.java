package org.project.appointment_project.user.service;

import org.project.appointment_project.user.dto.request.DoctorRegistrationRequest;
import org.project.appointment_project.user.dto.request.PatientRegistrationRequest;

public interface UserRegistrationValidator {
    void validatePatientRegistration(PatientRegistrationRequest request);
    void validateDoctorRegistration(DoctorRegistrationRequest request);
}
