package org.project.appointment_project.user.service;

import org.project.appointment_project.common.dto.PageResponse;
import org.project.appointment_project.user.dto.request.SpecialtyRequest;
import org.project.appointment_project.user.dto.request.SpecialtyUpdate;
import org.project.appointment_project.user.dto.response.SpecialtyResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface SpecialtyService {

    SpecialtyResponse createSpecialty(SpecialtyRequest request);

    SpecialtyResponse getSpecialtyById(UUID id);

    List<SpecialtyResponse> getAllActiveSpecialties();

    PageResponse<SpecialtyResponse> getSpecialtiesWithFilters(String name, Boolean isActive, Pageable pageable);

    SpecialtyResponse updateSpecialty(UUID id, SpecialtyUpdate updateDto);

    void deleteSpecialty(UUID id);
}
