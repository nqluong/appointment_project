package org.project.appointment_project.medicalrecord.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.common.dto.PageResponse;
import org.project.appointment_project.common.security.annotation.RequireOwnershipOrAdmin;
import org.project.appointment_project.medicalrecord.dto.response.MedicalRecordResponse;
import org.project.appointment_project.medicalrecord.dto.response.MedicalRecordSummaryResponse;
import org.project.appointment_project.medicalrecord.service.MedicalRecordService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/medical-records")
@RequiredArgsConstructor
@Slf4j
public class MedicalRecordController {
    private final MedicalRecordService medicalRecordService;

    @GetMapping("/appointment/{appointmentId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'PATIENT', 'ADMIN')")
    public ResponseEntity<MedicalRecordResponse> getMedicalRecordByAppointment(
            @PathVariable UUID appointmentId) {


        MedicalRecordResponse response = medicalRecordService.getMedicalRecordByAppointmentId(appointmentId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/summaries/doctor/{doctorId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR')")
    public ResponseEntity<PageResponse<MedicalRecordSummaryResponse>> getMedicalRecordSummariesByDoctorId(
            @PathVariable UUID doctorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        PageResponse<MedicalRecordSummaryResponse> response =
                medicalRecordService.getMedicalRecordSummariesByDoctorId(doctorId, pageable);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/summaries/patient/{patientId}")
    @RequireOwnershipOrAdmin(userIdParam = "patientId", allowedRoles = {"PATIENT", "DOCTOR", "ADMIN"})
    public ResponseEntity<PageResponse<MedicalRecordSummaryResponse>> getMedicalRecordSummariesByPatientId(
            @PathVariable UUID patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        PageResponse<MedicalRecordSummaryResponse> response =
                medicalRecordService.getMedicalRecordSummariesByPatientId(patientId, pageable);

        return ResponseEntity.ok(response);
    }

    /**
     * Kiểm tra appointment đã có medical record chưa
     */
    @GetMapping("/appointment/{appointmentId}/exists")
    @PreAuthorize("hasAnyRole('DOCTOR', 'PATIENT', 'ADMIN')")
    public ResponseEntity<Boolean> checkMedicalRecordExists(
            @PathVariable UUID appointmentId) {

        log.debug("Checking if appointment {} has medical record", appointmentId);

        boolean exists = medicalRecordService.hasMedicalRecord(appointmentId);

        return ResponseEntity.ok(exists);
    }


    /**
     * Start examination - chuyển appointment status thành IN_PROGRESS
     */
    @PutMapping("/appointment/{appointmentId}/start-examination")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<String> startExamination(
            @PathVariable UUID appointmentId,
            Authentication authentication) {

        log.info("Starting examination for appointment {} by doctor {}", appointmentId, authentication.getName());

        // TODO: Implement logic to update appointment status to IN_PROGRESS
        // This would typically call appointmentService.updateAppointmentStatus(appointmentId, Status.IN_PROGRESS)

        return ResponseEntity.ok("Examination started successfully");
    }

}
