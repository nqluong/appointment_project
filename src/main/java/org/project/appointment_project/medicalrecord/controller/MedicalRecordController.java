package org.project.appointment_project.medicalrecord.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.appoinment.enums.Status;
import org.project.appointment_project.common.dto.PageResponse;
import org.project.appointment_project.common.security.annotation.RequireOwnershipOrAdmin;
import org.project.appointment_project.medicalrecord.dto.response.MedicalRecordResponse;
import org.project.appointment_project.medicalrecord.dto.response.MedicalRecordSummaryResponse;
import org.project.appointment_project.medicalrecord.service.MedicalRecordService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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

    @GetMapping("/{recordId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'PATIENT', 'ADMIN')")
    public ResponseEntity<MedicalRecordResponse> getMedicalRecordById(
            @PathVariable UUID recordId) {
        MedicalRecordResponse response = medicalRecordService.getMedicalRecordById(recordId);

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

    @GetMapping("/appointment/{appointmentId}/exists")
    @PreAuthorize("hasAnyRole('DOCTOR', 'PATIENT', 'ADMIN')")
    public ResponseEntity<Boolean> checkMedicalRecordExists(
            @PathVariable UUID appointmentId) {

        log.debug("Checking if appointment {} has medical record", appointmentId);

        boolean exists = medicalRecordService.hasMedicalRecord(appointmentId);

        return ResponseEntity.ok(exists);
    }

    @GetMapping("/summaries/filters")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR')")
    public ResponseEntity<PageResponse<MedicalRecordSummaryResponse>> getMedicalRecordSummariesWithFilters(
            @RequestParam(required = false) UUID doctorId,
            @RequestParam(required = false) UUID patientId,
            @RequestParam(required = false) UUID specialtyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Status status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {


        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        PageResponse<MedicalRecordSummaryResponse> response = medicalRecordService
                .getMedicalRecordSummariesWithFilters(doctorId, patientId, specialtyId, fromDate, toDate, status, pageable);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/summaries/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR')")
    public ResponseEntity<PageResponse<MedicalRecordSummaryResponse>> searchMedicalRecordSummaries(
            @RequestParam String searchTerm,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        PageResponse<MedicalRecordSummaryResponse> response =
                medicalRecordService.searchMedicalRecordSummaries(searchTerm, pageable);

        return ResponseEntity.ok(response);
    }


    @GetMapping("/summaries/recent")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR')")
    public ResponseEntity<PageResponse<MedicalRecordSummaryResponse>> getRecentMedicalRecordSummaries(
            @RequestParam(defaultValue = "7") @Min(1) @Max(365) int days,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        PageResponse<MedicalRecordSummaryResponse> response =
                medicalRecordService.getRecentMedicalRecordSummaries(days, pageable);

        return ResponseEntity.ok(response);
    }


    @GetMapping("/summaries/specialty/{specialtyId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR')")
    public ResponseEntity<PageResponse<MedicalRecordSummaryResponse>> getMedicalRecordSummariesBySpecialtyId(
            @PathVariable UUID specialtyId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        PageResponse<MedicalRecordSummaryResponse> response =
                medicalRecordService.getMedicalRecordSummariesBySpecialtyId(specialtyId, pageable);

        return ResponseEntity.ok(response);
    }

}
