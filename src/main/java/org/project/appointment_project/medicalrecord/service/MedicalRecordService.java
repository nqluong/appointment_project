package org.project.appointment_project.medicalrecord.service;

import org.project.appointment_project.appoinment.enums.Status;
import org.project.appointment_project.common.dto.PageResponse;
import org.project.appointment_project.medicalrecord.dto.request.CreateMedicalRecordRequest;
import org.project.appointment_project.medicalrecord.dto.request.UpdateMedicalRecordRequest;
import org.project.appointment_project.medicalrecord.dto.response.MedicalRecordResponse;
import org.project.appointment_project.medicalrecord.dto.response.MedicalRecordSummaryResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

public interface MedicalRecordService {

    // Tạo hồ sơ bệnh án mới cho cuộc hẹn
    MedicalRecordResponse createMedicalRecord(CreateMedicalRecordRequest request);

    // Cập nhật hồ sơ bệnh án hiện có
    MedicalRecordResponse updateMedicalRecord(UUID recordId, UpdateMedicalRecordRequest request);

    // Lấy hồ sơ bệnh án theo ID
    MedicalRecordResponse getMedicalRecordById(UUID recordId);

    // Lấy hồ sơ bệnh án theo ID cuộc hẹn
    MedicalRecordResponse getMedicalRecordByAppointmentId(UUID appointmentId);

    boolean hasMedicalRecord(UUID appointmentId);

    // Lấy danh sách tóm tắt hồ sơ bệnh án theo doctorId
    PageResponse<MedicalRecordSummaryResponse> getMedicalRecordSummariesByDoctorId(UUID doctorId, Pageable pageable);

    // Lấy danh sách tóm tắt hồ sơ bệnh án theo patientId
    PageResponse<MedicalRecordSummaryResponse> getMedicalRecordSummariesByPatientId(UUID patientId, Pageable pageable);

    // Lấy danh sách tóm tắt hồ sơ bệnh án với các bộ lọc
    PageResponse<MedicalRecordSummaryResponse> getMedicalRecordSummariesWithFilters(
            UUID doctorId, UUID patientId, UUID specialtyId,
            LocalDate fromDate, LocalDate toDate, Status status,
            Pageable pageable);

    // Tìm kiếm danh sách tóm tắt hồ sơ bệnh án
    PageResponse<MedicalRecordSummaryResponse> searchMedicalRecordSummaries(String searchTerm, Pageable pageable);

    // Lấy danh sách tóm tắt hồ sơ bệnh án gần đây
    PageResponse<MedicalRecordSummaryResponse> getRecentMedicalRecordSummaries(int days, Pageable pageable);

    // Lấy danh sách tóm tắt hồ sơ bệnh án theo chuyên khoa
    PageResponse<MedicalRecordSummaryResponse> getMedicalRecordSummariesBySpecialtyId(UUID specialtyId, Pageable pageable);

}
