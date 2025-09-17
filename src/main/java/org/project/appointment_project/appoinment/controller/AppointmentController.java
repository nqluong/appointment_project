package org.project.appointment_project.appoinment.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.appoinment.dto.request.CreateAppointmentRequest;
import org.project.appointment_project.appoinment.dto.response.AppointmentResponse;
import org.project.appointment_project.appoinment.enums.Status;
import org.project.appointment_project.appoinment.service.AppointmentService;
import org.project.appointment_project.common.dto.PageResponse;
import org.project.appointment_project.common.security.annotation.RequireOwnershipOrAdmin;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/appointments")
@Slf4j
public class AppointmentController {
    private final AppointmentService appointmentService;

    @PostMapping
    public ResponseEntity<AppointmentResponse> createAppointment(@Valid @RequestBody CreateAppointmentRequest request) {
        AppointmentResponse appointmentResponse = appointmentService.createAppointment(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(appointmentResponse);
    }

    @GetMapping
    @RequireOwnershipOrAdmin
    public ResponseEntity<PageResponse<AppointmentResponse>> getAppointments(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) Status status,
            @RequestParam(defaultValue = "0") @Min(0) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @RequestParam(defaultValue = "appointmentDate") String sortBy) {

        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        PageResponse<AppointmentResponse> response = appointmentService.getAppointments(userId, status, pageable);

        return ResponseEntity.ok(response);
    }

}
