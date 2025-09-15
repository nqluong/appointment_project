package org.project.appointment_project.schedule.service;

import java.time.LocalDate;
import java.util.UUID;

import org.project.appointment_project.common.dto.PageResponse;
import org.project.appointment_project.schedule.dto.response.DoctorWithSlotsResponse;
import org.springframework.data.domain.Pageable;

public interface DoctorAvailabilityService {
    /**
     * Lấy danh sách bác sĩ với các khung giờ có sẵn 
     *
     * @param startDate Ngày bắt đầu 
     * @param endDate Ngày kết thúc
     * @param pageable Tham số phân trang
     * @return Phản hồi phân trang chứa các bác sĩ với khung giờ có sẵn
     */
    PageResponse<DoctorWithSlotsResponse> getDoctorsWithAvailableSlots(
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    );

    /**
     * Lấy danh sách bác sĩ được lọc theo chuyên khoa với các khung giờ có sẵn
     *
     * @param specialtyId ID chuyên khoa để lọc
     * @param startDate Ngày bắt đầu 
     * @param endDate Ngày kết thúc 
     * @param pageable Tham số phân trang
     * @return Phản hồi phân trang chứa các bác sĩ với khung giờ có sẵn
     */
    PageResponse<DoctorWithSlotsResponse> getDoctorsWithAvailableSlotsBySpecialty(
            UUID specialtyId,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    );
}
