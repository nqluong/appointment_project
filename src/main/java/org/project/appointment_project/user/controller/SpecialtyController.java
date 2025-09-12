package org.project.appointment_project.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.user.dto.request.SpecialtyRequest;
import org.project.appointment_project.user.dto.request.SpecialtyUpdate;
import org.project.appointment_project.user.dto.response.SpecialtyResponse;
import org.project.appointment_project.user.service.SpecialtyService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/specialties")
@RequiredArgsConstructor
@Slf4j
public class SpecialtyController {
    private final SpecialtyService specialtyService;

    @PostMapping
    public ResponseEntity<SpecialtyResponse> createSpecialty(
            @Valid @RequestBody SpecialtyRequest requestDto) {
        log.info("Request to create specialty: {}", requestDto.getName());

        SpecialtyResponse createdSpecialty = specialtyService.createSpecialty(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdSpecialty);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SpecialtyResponse> getSpecialtyById(
            @PathVariable UUID id) {
        log.info("Request to get specialty by id: {}", id);

        SpecialtyResponse specialty = specialtyService.getSpecialtyById(id);
        return ResponseEntity.ok(specialty);
    }

    @GetMapping("/active")
    public ResponseEntity<List<SpecialtyResponse>> getAllActiveSpecialties() {
        log.info("Request to get all active specialties");

        List<SpecialtyResponse> specialties = specialtyService.getAllActiveSpecialties();
        return ResponseEntity.ok(specialties);
    }

    @GetMapping
    public ResponseEntity<Page<SpecialtyResponse>> getSpecialtiesWithFilters(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Request to get specialties with filters - name: {}, isActive: {}", name, isActive);
        Pageable pageable = PageRequest.of(page, size);
        Page<SpecialtyResponse> specialties = specialtyService.getSpecialtiesWithFilters(name, isActive, pageable);
        return ResponseEntity.ok(specialties);
    }

    @PutMapping("/{id}")

    public ResponseEntity<SpecialtyResponse> updateSpecialty(
            @PathVariable UUID id,
            @Valid @RequestBody SpecialtyUpdate updateDto) {
        log.info("Request to update specialty with id: {}", id);

        SpecialtyResponse updatedSpecialty = specialtyService.updateSpecialty(id, updateDto);
        return ResponseEntity.ok(updatedSpecialty);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSpecialty(
          @PathVariable UUID id) {
        log.info("Request to delete specialty with id: {}", id);

        specialtyService.deleteSpecialty(id);
        return ResponseEntity.noContent().build();
    }
}
