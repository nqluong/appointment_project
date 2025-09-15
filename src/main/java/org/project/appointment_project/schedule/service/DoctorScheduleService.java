package org.project.appointment_project.schedule.service;

import org.project.appointment_project.common.dto.PageResponse;
import org.project.appointment_project.schedule.dto.request.DoctorScheduleCreateRequest;
import org.project.appointment_project.schedule.dto.request.DoctorSearchRequest;
import org.project.appointment_project.schedule.dto.response.DoctorScheduleResponse;
import org.project.appointment_project.schedule.dto.response.DoctorSearchResponse;

import java.util.UUID;

public interface DoctorScheduleService {
    DoctorScheduleResponse createDoctorSchedule(DoctorScheduleCreateRequest request);
    DoctorScheduleResponse getDoctorSchedule(UUID doctorId);
    DoctorScheduleResponse updateDoctorSchedule(UUID doctorId, DoctorScheduleCreateRequest request);
    void deleteDoctorSchedule(UUID doctorId);
    PageResponse<DoctorSearchResponse> searchDoctors(DoctorSearchRequest request);
}
