package org.project.appointment_project.schedule.service;

import org.project.appointment_project.schedule.dto.request.DoctorSearchRequest;

public interface SearchRequestValidator {
    /**
     * Validate các tham số tìm kiếm
     * @param request yêu cầu tìm kiếm cần validate
     */
    void validateSearchRequest(DoctorSearchRequest request);
}
