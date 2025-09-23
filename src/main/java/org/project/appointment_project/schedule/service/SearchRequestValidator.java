package org.project.appointment_project.schedule.service;

import org.project.appointment_project.schedule.dto.request.DoctorSearchRequest;

public interface SearchRequestValidator {
    // Validate các tham số tìm kiếm
    void validateSearchRequest(DoctorSearchRequest request);
}
