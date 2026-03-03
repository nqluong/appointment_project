package org.project.appointment_project.user.service;

import java.util.UUID;

import org.project.appointment_project.common.dto.PageResponse;
import org.project.appointment_project.user.dto.response.DoctorResponse;
import org.springframework.data.domain.Pageable;

public interface DoctorService {

    PageResponse<DoctorResponse> getAllDoctors(Pageable pageable);

    PageResponse<DoctorResponse> getDoctorsWithFilters(String specialtyName, Pageable pageable);

    DoctorResponse getDoctorById(UUID doctorId);
}
