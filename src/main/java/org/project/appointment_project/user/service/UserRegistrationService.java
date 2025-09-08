package org.project.appointment_project.user.service;

import org.project.appointment_project.user.dto.request.DoctorRegistrationRequest;
import org.project.appointment_project.user.dto.request.PatientRegistrationRequest;
import org.project.appointment_project.user.dto.response.UserRegistrationResponse;

public interface UserRegistrationService {
    UserRegistrationResponse registerPatient(PatientRegistrationRequest request);

    UserRegistrationResponse registerDoctor(DoctorRegistrationRequest request);
}
