package org.project.appointment_project.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.project.appointment_project.user.dto.request.DoctorRegistrationRequest;
import org.project.appointment_project.user.dto.request.PatientRegistrationRequest;
import org.project.appointment_project.user.dto.response.UserRegistrationResponse;
import org.project.appointment_project.user.service.UserRegistrationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/register")
@RequiredArgsConstructor
public class UserRegistrationController {
    private final UserRegistrationService userRegistrationService;

    @PostMapping("patient")
    public ResponseEntity<UserRegistrationResponse> registerPatient(
            @Valid @RequestBody PatientRegistrationRequest request) {

        UserRegistrationResponse response = userRegistrationService.registerPatient(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/doctor")
    public ResponseEntity<UserRegistrationResponse> registerDoctor(
            @Valid @RequestBody DoctorRegistrationRequest request) {

        UserRegistrationResponse response = userRegistrationService.registerDoctor(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
