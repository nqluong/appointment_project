package org.project.appointment_project.user.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.common.exception.CustomException;
import org.project.appointment_project.common.exception.ErrorCode;
import org.project.appointment_project.user.dto.request.SpecialtyRequest;
import org.project.appointment_project.user.dto.request.SpecialtyUpdate;
import org.project.appointment_project.user.dto.response.SpecialtyResponse;
import org.project.appointment_project.user.mapper.SpecialtyMapper;
import org.project.appointment_project.user.model.Specialty;
import org.project.appointment_project.user.repository.SpecialtyRepository;
import org.project.appointment_project.user.service.SpecialtyService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SpecialtyServiceImpl implements SpecialtyService {

    SpecialtyRepository specialtyRepository;
    SpecialtyMapper specialtyMapper;


    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public SpecialtyResponse createSpecialty(SpecialtyRequest request) {

        if (specialtyRepository.existsByName(request.getName())) {
            throw new CustomException(ErrorCode.SPECIALTY_NAME_EXISTS);
        }

        Specialty specialty = specialtyMapper.toEntity(request);
        if (specialty.getIsActive() == null) {
            specialty.setIsActive(true);
        }

        Specialty savedSpecialty = specialtyRepository.save(specialty);

        return specialtyMapper.toResponseDto(savedSpecialty);
    }

    @Override
    @Transactional(readOnly = true)
    public SpecialtyResponse getSpecialtyById(UUID id) {
        Specialty specialty = findSpecialtyById(id);
        return specialtyMapper.toResponseDto(specialty);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpecialtyResponse> getAllActiveSpecialties() {
        List<Specialty> specialties = specialtyRepository.findByIsActiveTrue();
        return specialtyMapper.toResponseDtoList(specialties);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SpecialtyResponse> getSpecialtiesWithFilters(String name, Boolean isActive, Pageable pageable) {
        Page<Specialty> specialties = specialtyRepository.findSpecialtiesWithFilters(name, isActive, pageable);
        return specialties.map(specialtyMapper::toResponseDto);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public SpecialtyResponse updateSpecialty(UUID id, SpecialtyUpdate updateDto) {

        Specialty existingSpecialty = findSpecialtyById(id);

        // Check for duplicate name if name is being updated
        if (updateDto.getName() != null &&
                !updateDto.getName().equals(existingSpecialty.getName()) &&
                specialtyRepository.existsByNameAndIdNot(updateDto.getName(), id)) {
            throw new CustomException(ErrorCode.SPECIALTY_NAME_EXISTS);

        }

        specialtyMapper.updateEntityFromDto(updateDto, existingSpecialty);
        Specialty updatedSpecialty = specialtyRepository.save(existingSpecialty);

        return specialtyMapper.toResponseDto(updatedSpecialty);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteSpecialty(UUID id) {

        Specialty specialty = findSpecialtyById(id);

        specialty.setIsActive(false);
        specialtyRepository.save(specialty);

        log.info("Successfully soft deleted specialty with id: {}", id);
    }

    private Specialty findSpecialtyById(UUID id) {
        return specialtyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Specialty not found with id: " + id
                ));
    }
}
