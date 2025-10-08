package org.project.appointment_project.schedule.controller;

import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.Parameter;
import org.project.appointment_project.common.dto.PageResponse;
import org.project.appointment_project.schedule.dto.response.DoctorWithSlotsResponse;
import org.project.appointment_project.schedule.service.DoctorAvailabilityService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
public class DoctorAvailabilityController {

    private final DoctorAvailabilityService doctorAvailabilityService;

    @GetMapping("/with-available-slots")
    public ResponseEntity<PageResponse<DoctorWithSlotsResponse>> getDoctorsWithAvailableSlots(
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now()}")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().plusDays(30)}")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "firstName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        PageResponse<DoctorWithSlotsResponse> response = doctorAvailabilityService
                .getDoctorsWithAvailableSlots(startDate, endDate, pageable);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/specialty/{specialtyId}/with-available-slots")
    public ResponseEntity<PageResponse<DoctorWithSlotsResponse>> getDoctorsWithAvailableSlotsBySpecialty(
            @PathVariable UUID specialtyId,
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now()}")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().plusDays(30)}")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "firstName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        PageResponse<DoctorWithSlotsResponse> response = doctorAvailabilityService
                .getDoctorsWithAvailableSlotsBySpecialty(specialtyId, startDate, endDate, pageable);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{doctorId}/available-slots")
    public ResponseEntity<DoctorWithSlotsResponse> getDoctorAvailableSlots(
            @PathVariable UUID doctorId,
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now()}")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().plusDays(7)}")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        DoctorWithSlotsResponse response = doctorAvailabilityService
                .getDoctorAvailableSlots1(doctorId, startDate, endDate);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{doctorId}/available-slots/cache")
    public ResponseEntity<DoctorWithSlotsResponse> getDoctorAvailableSlots2(
            @PathVariable UUID doctorId,
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now()}")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().plusDays(7)}")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        DoctorWithSlotsResponse response = doctorAvailabilityService
                .getDoctorAvailableSlots2(doctorId, startDate, endDate);

        return ResponseEntity.ok(response);
    }
}
