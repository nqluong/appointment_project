package org.project.appointment_project.medicalrecord.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.project.appointment_project.appoinment.enums.Status;
import org.project.appointment_project.appoinment.model.Appointment;
import org.project.appointment_project.appoinment.repository.AppointmentRepository;
import org.project.appointment_project.common.dto.PageResponse;
import org.project.appointment_project.common.exception.CustomException;
import org.project.appointment_project.common.exception.ErrorCode;
import org.project.appointment_project.common.mapper.PageMapper;
import org.project.appointment_project.medicalrecord.dto.request.CreateMedicalRecordRequest;
import org.project.appointment_project.medicalrecord.dto.request.UpdateMedicalRecordRequest;
import org.project.appointment_project.medicalrecord.dto.response.MedicalRecordResponse;
import org.project.appointment_project.medicalrecord.dto.response.MedicalRecordSummaryResponse;
import org.project.appointment_project.medicalrecord.mapper.MedicalRecordMapper;
import org.project.appointment_project.medicalrecord.model.MedicalRecord;
import org.project.appointment_project.medicalrecord.repository.MedicalRecordProjection;
import org.project.appointment_project.medicalrecord.repository.MedicalRecordRepository;
import org.project.appointment_project.medicalrecord.service.MedicalRecordService;
import org.project.appointment_project.medicalrecord.service.MedicalRecordValidationService;
import org.project.appointment_project.user.model.User;
import org.project.appointment_project.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MedicalRecordServiceImpl implements MedicalRecordService {

    MedicalRecordRepository medicalRecordRepository;
    AppointmentRepository appointmentRepository;
    UserRepository userRepository;
    MedicalRecordValidationService validationService;
    MedicalRecordMapper medicalRecordMapper;
    PageMapper pageMapper;
    ProjectionMapperService projectionMapperService;

    @Override
    @Transactional
    public MedicalRecordResponse createMedicalRecord(CreateMedicalRecordRequest request) {
        try {

            // Lấy appointment với lock để tránh concurrent access
            Appointment appointment = getAppointmentWithLock(request.getAppointmentId());

            validationService.validateMedicalRecordCreation(appointment);

            MedicalRecord medicalRecord = medicalRecordMapper.toEntity(request);
            medicalRecord.setAppointment(appointment);

            medicalRecord = medicalRecordRepository.save(medicalRecord);

            updateAppointmentAfterMedicalRecord(appointment, request.getDoctorNotes());


            return getMedicalRecordResponseByAppointmentId(request.getAppointmentId());

        } catch (CustomException e) {
            log.error("Không thể tạo hồ sơ bệnh án: {} - {}", e.getErrorCode().getCode(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Lỗi không mong đợi khi tạo hồ sơ bệnh án cho cuộc hẹn {}",
                    request.getAppointmentId(), e);
            throw new CustomException(ErrorCode.MEDICAL_RECORD_CREATION_FAILED);
        }
    }

    @Override
    @Transactional
    public MedicalRecordResponse updateMedicalRecord(UUID recordId, UpdateMedicalRecordRequest request) {
        try {

            MedicalRecord medicalRecord = getMedicalRecordWithLock(recordId);

            // Validate quyền cập nhật
            validationService.validateUpdatePermission(medicalRecord.getAppointment());

            medicalRecordMapper.updateEntity(request, medicalRecord);

            if (request.getDoctorNotes() != null) {
                medicalRecord.getAppointment().setDoctorNotes(request.getDoctorNotes());
                appointmentRepository.save(medicalRecord.getAppointment());
            }
            medicalRecord = medicalRecordRepository.save(medicalRecord);

            log.info("Đã cập nhật thành công hồ sơ bệnh án {}", recordId);

            return getMedicalRecordResponseByAppointmentId(medicalRecord.getAppointment().getId());

        } catch (CustomException e) {
            log.error("Không thể cập nhật hồ sơ bệnh án: {} - {}", e.getErrorCode().getCode(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Lỗi không mong đợi khi cập nhật hồ sơ bệnh án {}", recordId, e);
            throw new CustomException(ErrorCode.MEDICAL_RECORD_UPDATE_FAILED);
        }
    }

    @Override
    public MedicalRecordResponse getMedicalRecordById(UUID recordId) {
        try {
            MedicalRecord medicalRecord = getMedicalRecordRepoById(recordId);

            // Validate quyền xem
            validationService.validateViewPermission(medicalRecord.getAppointment());

           MedicalRecordProjection projection = medicalRecordRepository.findProjectionByMedicalRecordId(recordId)
                   .orElseThrow(() -> new CustomException(ErrorCode.MEDICAL_RECORD_NOT_FOUND));

           return projectionMapperService.toMedicalRecordResponse(projection);
        } catch (CustomException e) {
            log.error("Không thể lấy hồ sơ bệnh án: {} - {}", e.getErrorCode().getCode(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Lỗi không mong đợi khi lấy hồ sơ bệnh án {}", recordId, e);
            throw new CustomException(ErrorCode.MEDICAL_RECORD_FETCH_FAILED);
        }
    }

    @Override
    public MedicalRecordResponse getMedicalRecordByAppointmentId(UUID appointmentId) {
        try {
            MedicalRecord medicalRecord = medicalRecordRepository.findByAppointmentId(appointmentId)
                    .orElseThrow(() -> new CustomException(ErrorCode.MEDICAL_RECORD_NOT_FOUND));

            validationService.validateViewPermission(medicalRecord.getAppointment());

            return getMedicalRecordResponseByAppointmentId(appointmentId);

        } catch (CustomException e) {
            log.error("Không thể lấy hồ sơ bệnh án theo cuộc hẹn: {} - {}", e.getErrorCode().getCode(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Lỗi không mong đợi khi lấy hồ sơ bệnh án cho cuộc hẹn {}", appointmentId, e);
            throw new CustomException(ErrorCode.MEDICAL_RECORD_FETCH_FAILED);
        }
    }

    @Override
    public PageResponse<MedicalRecordSummaryResponse> getMedicalRecordSummariesByDoctorId(UUID doctorId, Pageable pageable) {
        try {
            validationService.validateSummaryAccess(false);
            doctorId = validationService.applyUserBasedFiltering(doctorId, "doctorId");
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
            Page<MedicalRecordSummaryResponse> summaries = medicalRecordRepository.findSummariesByDoctorId(doctorId, sevenDaysAgo, pageable);

            return pageMapper.toPageResponse(summaries);

        } catch (CustomException e) {
            log.error("Không thể lấy danh sách tóm tắt hồ sơ bệnh án theo ID bác sĩ: {} - {}", e.getErrorCode().getCode(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Lỗi không mong đợi khi lấy danh sách tóm tắt hồ sơ bệnh án của bác sĩ {}", doctorId, e);
            throw new CustomException(ErrorCode.MEDICAL_RECORD_FETCH_FAILED);
        }
    }

    @Override
    public PageResponse<MedicalRecordSummaryResponse> getMedicalRecordSummariesByPatientId(UUID patientId, Pageable pageable) {
        try {
            validationService.validateSummaryAccess(true); // Allow patients
            patientId = validationService.applyUserBasedFiltering(patientId, "patientId");

            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
            Page<MedicalRecordSummaryResponse> summaries = medicalRecordRepository.findSummariesByPatientId(patientId, sevenDaysAgo, pageable);

            return pageMapper.toPageResponse(summaries);

        } catch (CustomException e) {
            log.error("Không thể lấy danh sách tóm tắt hồ sơ bệnh án theo ID bệnh nhân: {} - {}", e.getErrorCode().getCode(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Lỗi không mong đợi khi lấy danh sách tóm tắt hồ sơ bệnh án cho bệnh nhân {}", patientId, e);
            throw new CustomException(ErrorCode.MEDICAL_RECORD_FETCH_FAILED);
        }
    }

    @Override
    public PageResponse<MedicalRecordSummaryResponse> getMedicalRecordSummariesWithFilters(UUID doctorId, UUID patientId, UUID specialtyId, LocalDate fromDate, LocalDate toDate, Status status, Pageable pageable) {
        try {
            validationService.validateSummaryAccess(false);
            doctorId = validationService.applyUserBasedFiltering(doctorId, "doctorId");
            patientId = validationService.applyUserBasedFiltering(patientId, "patientId");

            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
            Page<MedicalRecordSummaryResponse> summaries = medicalRecordRepository.findSummariesWithFilters(
                    doctorId, patientId, specialtyId, fromDate, toDate, status, sevenDaysAgo, pageable);
            return pageMapper.toPageResponse(summaries);

        } catch (CustomException e) {
            log.error("Không thể lấy danh sách tóm tắt hồ sơ bệnh án đã lọc: {} - {}", e.getErrorCode().getCode(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Lỗi không mong đợi khi lấy danh sách tóm tắt hồ sơ bệnh án đã lọc", e);
            throw new CustomException(ErrorCode.MEDICAL_RECORD_FETCH_FAILED);
        }
    }

    @Override
    public PageResponse<MedicalRecordSummaryResponse> searchMedicalRecordSummaries(String searchTerm, Pageable pageable) {
        try {
            validationService.validateSummaryAccess(false);

            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
            Page<MedicalRecordSummaryResponse> summaries = medicalRecordRepository.searchMedicalRecordSummaries(searchTerm, sevenDaysAgo, pageable);

            return pageMapper.toPageResponse(summaries);

        } catch (CustomException e) {
            log.error("Không thể tìm kiếm danh sách tóm tắt hồ sơ bệnh án: {} - {}", e.getErrorCode().getCode(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Lỗi không mong đợi khi tìm kiếm danh sách tóm tắt hồ sơ bệnh án với từ khóa: {}", searchTerm, e);
            throw new CustomException(ErrorCode.MEDICAL_RECORD_FETCH_FAILED);
        }
    }

    @Override
    public PageResponse<MedicalRecordSummaryResponse> getRecentMedicalRecordSummaries(int days, Pageable pageable) {
        try {
            validationService.validateSummaryAccess(false);

            LocalDateTime dateTime = LocalDateTime.now().plusDays(days);
            Page<MedicalRecordSummaryResponse> summaries = medicalRecordRepository.findRecentMedicalRecordSummaries(dateTime, pageable);

            return pageMapper.toPageResponse(summaries);

        } catch (CustomException e) {
            log.error("Không thể lấy danh sách tóm tắt hồ sơ bệnh án gần đây: {} - {}", e.getErrorCode().getCode(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Lỗi không mong đợi khi lấy danh sách tóm tắt hồ sơ bệnh án gần đây", e);
            throw new CustomException(ErrorCode.MEDICAL_RECORD_FETCH_FAILED);
        }
    }

    @Override
    public PageResponse<MedicalRecordSummaryResponse> getMedicalRecordSummariesBySpecialtyId(UUID specialtyId, Pageable pageable) {
        try {
            validationService.validateSummaryAccess(false);

            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
            Page<MedicalRecordSummaryResponse> summaries = medicalRecordRepository.findSummariesBySpecialtyId(specialtyId, sevenDaysAgo, pageable);

            return pageMapper.toPageResponse(summaries);

        } catch (CustomException e) {
            log.error("Không thể lấy danh sách tóm tắt hồ sơ bệnh án theo chuyên khoa: {} - {}", e.getErrorCode().getCode(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Lỗi không mong đợi khi lấy danh sách tóm tắt hồ sơ bệnh án cho chuyên khoa {}", specialtyId, e);
            throw new CustomException(ErrorCode.MEDICAL_RECORD_FETCH_FAILED);
        }
    }

    @Override
    public boolean hasMedicalRecord(UUID appointmentId) {
        return medicalRecordRepository.existsByAppointmentId(appointmentId);
    }

    private MedicalRecordResponse getMedicalRecordResponseByAppointmentId(UUID appointmentId) {
        MedicalRecordProjection projection = medicalRecordRepository.findProjectionByAppointmentId(appointmentId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEDICAL_RECORD_NOT_FOUND));

        return projectionMapperService.toMedicalRecordResponse(projection);
    }

    private User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private Appointment getAppointmentWithLock(UUID appointmentId) {
        return appointmentRepository.findByIdWithLock(appointmentId)
                .orElseThrow(() -> new CustomException(ErrorCode.APPOINTMENT_NOT_FOUND));
    }

    private MedicalRecord getMedicalRecordWithLock(UUID recordId) {
        return medicalRecordRepository.findById(recordId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEDICAL_RECORD_NOT_FOUND));
    }

    private MedicalRecord getMedicalRecordRepoById(UUID recordId) {
        return medicalRecordRepository.findById(recordId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEDICAL_RECORD_NOT_FOUND));
    }


    /**
     * Cập nhật cuộc hẹn sau khi tạo hồ sơ bệnh án
     * Chuyển trạng thái thành ĐÃ HOÀN THÀNH và cập nhật ghi chú của bác sĩ
     */
    private void updateAppointmentAfterMedicalRecord(Appointment appointment, String doctorNotes) {
        appointment.setStatus(Status.COMPLETED);
        if (doctorNotes != null && !doctorNotes.trim().isEmpty()) {
            appointment.setDoctorNotes(doctorNotes);
        }
        appointmentRepository.save(appointment);

        log.debug("Đã cập nhật trạng thái cuộc hẹn {} thành ĐÃ HOÀN THÀNH", appointment.getId());
    }

}
