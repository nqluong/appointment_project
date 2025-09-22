package org.project.appointment_project.schedule.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.common.dto.PageResponse;
import org.project.appointment_project.common.exception.CustomException;
import org.project.appointment_project.common.exception.ErrorCode;
import org.project.appointment_project.common.mapper.PageMapper;
import org.project.appointment_project.common.util.SecurityUtils;
import org.project.appointment_project.schedule.dto.request.CreateAbsenceRequest;
import org.project.appointment_project.schedule.dto.request.UpdateAbsenceRequest;
import org.project.appointment_project.schedule.dto.response.DoctorAbsenceResponse;
import org.project.appointment_project.schedule.mapper.DoctorAbsenceMapper;
import org.project.appointment_project.schedule.model.DoctorAbsence;
import org.project.appointment_project.schedule.repository.DoctorAbsenceRepository;
import org.project.appointment_project.schedule.service.DoctorAbsenceService;
import org.project.appointment_project.schedule.validator.AbsenceValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DoctorAbsenceServiceImpl implements DoctorAbsenceService {

    DoctorAbsenceRepository doctorAbsenceRepository;
    DoctorAbsenceMapper doctorAbsenceMapper;
    AbsenceValidator absenceValidator;
    PageMapper pageMapper;
    SecurityUtils securityUtils;

    @Override
    public DoctorAbsenceResponse createAbsence(CreateAbsenceRequest request) {
        absenceValidator.validateCreateRequest(request);

        // Check for conflicts
        if (hasConflictingAbsence(request.getDoctorUserId(), request.getAbsenceDate(),
                request.getStartTime(), request.getEndTime(), null)) {
            throw new CustomException(ErrorCode.ABSENCE_CONFLICT);
        }

        // Create and save entity
        DoctorAbsence absence = doctorAbsenceMapper.toEntity(request);
        DoctorAbsence savedAbsence = doctorAbsenceRepository.save(absence);

        log.info("Created absence with ID: {}", savedAbsence.getId());
        return doctorAbsenceMapper.toDto(savedAbsence);
    }

    @Override
    public DoctorAbsenceResponse updateAbsence(UUID absenceId, UpdateAbsenceRequest request) {
        absenceValidator.validateUpdateRequest(request);

        // Get existing absence
        DoctorAbsence existingAbsence = getAbsenceEntityById(absenceId);

        validateOwnership(existingAbsence.getDoctor().getId());

        // Check for conflicts if date/time is being updated
        if (request.getAbsenceDate() != null || request.getStartTime() != null || request.getEndTime() != null) {
            LocalDate newDate = request.getAbsenceDate() != null ?
                    request.getAbsenceDate() : existingAbsence.getAbsenceDate();
            LocalTime newStartTime = request.getStartTime() != null ?
                    request.getStartTime() : existingAbsence.getStartTime();
            LocalTime newEndTime = request.getEndTime() != null ?
                    request.getEndTime() : existingAbsence.getEndTime();

            if (hasConflictingAbsence(existingAbsence.getDoctor().getId(), newDate,
                    newStartTime, newEndTime, absenceId)) {
               throw new CustomException(ErrorCode.ABSENCE_CONFLICT);
            }
        }

        // Update entity
        doctorAbsenceMapper.updateEntityFromRequest(request, existingAbsence);
        DoctorAbsence savedAbsence = doctorAbsenceRepository.save(existingAbsence);

        log.info("Updated absence: {}", absenceId);
        return doctorAbsenceMapper.toDto(savedAbsence);
    }

    @Override
    public DoctorAbsenceResponse getAbsenceById(UUID absenceId) {
        DoctorAbsence absence = getAbsenceEntityById(absenceId);
        return doctorAbsenceMapper.toDto(absence);
    }

    @Override
    public PageResponse<DoctorAbsenceResponse> getAbsencesByDoctor(UUID doctorUserId, Pageable pageable) {
        Page<DoctorAbsence> absences = doctorAbsenceRepository
                .findByDoctorIdOrderByAbsenceDateDesc(doctorUserId, pageable);
        return pageMapper.toPageResponse(absences, doctorAbsenceMapper::toDto);
    }

    @Override
    public List<DoctorAbsenceResponse> getAbsencesInDateRange(UUID doctorUserId, LocalDate startDate, LocalDate endDate) {
        List<DoctorAbsence> absences = doctorAbsenceRepository
                .findAbsencesInDateRange(doctorUserId, startDate, endDate);
        return doctorAbsenceMapper.toDtoList(absences);
    }

    @Override
    public List<DoctorAbsenceResponse> getFutureAbsences(UUID doctorUserId) {
        List<DoctorAbsence> absences = doctorAbsenceRepository
                .findFutureAbsences(doctorUserId, LocalDate.now());
        return doctorAbsenceMapper.toDtoList(absences);
    }

    @Override
    public void deleteAbsence(UUID absenceId) {
        DoctorAbsence absence = getAbsenceEntityById(absenceId);
        doctorAbsenceRepository.delete(absence);
        log.info("Deleted absence: {}", absenceId);
    }

    @Override
    public boolean isDoctorAbsentOnDate(UUID doctorUserId, LocalDate date) {
        List<DoctorAbsence> absences = doctorAbsenceRepository
                .findAbsencesInDateRange(doctorUserId, date, date);
        return !absences.isEmpty();
    }

    @Override
    public int cleanupPastAbsences(LocalDate cutoffDate) {
        int deletedCount = doctorAbsenceRepository.deletePastAbsences(cutoffDate);
        log.info("Deleted {} past absences", deletedCount);
        return deletedCount;
    }

    private DoctorAbsence getAbsenceEntityById(UUID absenceId) {
        return doctorAbsenceRepository.findById(absenceId)
                .orElseThrow(() -> new CustomException(ErrorCode.ABSENCE_NOT_FOUND));
    }

    private boolean hasConflictingAbsence(UUID doctorUserId, LocalDate absenceDate,
                                          LocalTime startTime, LocalTime endTime, UUID excludeId) {
        return doctorAbsenceRepository.existsConflictingAbsence(
                doctorUserId, absenceDate, startTime, endTime, excludeId
        );
    }

    private void validateOwnership(UUID doctorUserId) {
        UUID currentUserId = securityUtils.getCurrentUserId();

        if (securityUtils.isCurrentUserAdmin()) {
            return;
        }

        if (!currentUserId.equals(doctorUserId)) {
            log.warn("User {} attempted to modify absence of doctor {}", currentUserId, doctorUserId);
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }
}
