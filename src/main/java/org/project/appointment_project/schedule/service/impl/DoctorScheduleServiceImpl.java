package org.project.appointment_project.schedule.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.common.dto.PageResponse;
import org.project.appointment_project.common.exception.CustomException;
import org.project.appointment_project.common.exception.ErrorCode;
import org.project.appointment_project.common.mapper.PageMapper;
import org.project.appointment_project.schedule.dto.request.DoctorScheduleCreateRequest;
import org.project.appointment_project.schedule.dto.request.DoctorSearchRequest;
import org.project.appointment_project.schedule.dto.response.DoctorScheduleResponse;
import org.project.appointment_project.schedule.dto.response.DoctorSearchResponse;
import org.project.appointment_project.schedule.mapper.DoctorScheduleMapper;
import org.project.appointment_project.schedule.mapper.DoctorSearchMapper;
import org.project.appointment_project.schedule.model.DoctorSchedule;
import org.project.appointment_project.schedule.repository.DoctorScheduleRepository;
import org.project.appointment_project.schedule.service.DoctorScheduleService;
import org.project.appointment_project.schedule.service.DoctorSearchSpecificationService;
import org.project.appointment_project.schedule.service.ScheduleValidationService;
import org.project.appointment_project.user.model.User;
import org.project.appointment_project.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class DoctorScheduleServiceImpl implements DoctorScheduleService {

    DoctorScheduleRepository doctorScheduleRepository;
    UserRepository userRepository;
    DoctorScheduleMapper doctorScheduleMapper;
    DoctorSearchMapper doctorSearchMapper;
    PageMapper pageMapper;
    ScheduleValidationService scheduleValidationService;
    DoctorSearchSpecificationService specificationService;

    @Override
    @Transactional
    public DoctorScheduleResponse createDoctorSchedule(DoctorScheduleCreateRequest request) {
        // Validate doctor exists and has proper role
        User doctor = findAndValidateDoctor(request.getDoctorId());

        // Validate schedule entries
        scheduleValidationService.validateScheduleEntries(request.getScheduleEntries());

        // Check if doctor already has schedule
        if (doctorScheduleRepository.existsByDoctorId(doctor.getId())) {
            log.warn("Doctor {} already has a schedule", doctor.getId());
            throw new CustomException(ErrorCode.SCHEDULE_ALREADY_EXISTS);
        }

        // Create schedule entities
        List<DoctorSchedule> schedules = doctorScheduleMapper.toEntityList(
                request.getScheduleEntries(),
                doctor,
                request.getTimezone(),
                request.getNotes()
        );

        // Save schedules
        List<DoctorSchedule> savedSchedules = doctorScheduleRepository.saveAll(schedules);

        log.info("Successfully created {} schedule entries for doctor: {}",
                savedSchedules.size(), doctor.getId());

        return doctorScheduleMapper.toDoctorScheduleResponse(doctor, savedSchedules);
    }

    @Override
    @Transactional(readOnly = true)
    public DoctorScheduleResponse getDoctorSchedule(UUID doctorId) {
        User doctor = findAndValidateDoctor(doctorId);
        List<DoctorSchedule> schedules = doctorScheduleRepository.findByDoctorIdAndIsActiveTrue(doctorId);

        if (schedules.isEmpty()) {
            log.warn("No active schedule found for doctor: {}", doctorId);
            throw new CustomException(ErrorCode.SCHEDULE_NOT_FOUND);
        }

        return doctorScheduleMapper.toDoctorScheduleResponse(doctor, schedules);
    }

    @Override
    @Transactional
    public DoctorScheduleResponse updateDoctorSchedule(UUID doctorId, DoctorScheduleCreateRequest request) {
        User doctor = findAndValidateDoctor(doctorId);
        scheduleValidationService.validateScheduleEntries(request.getScheduleEntries());

        // Deactivate existing schedules
        doctorScheduleRepository.deactivateSchedulesByDoctorId(doctorId);

        // Create new schedule entries
        List<DoctorSchedule> newSchedules = doctorScheduleMapper.toEntityList(
                request.getScheduleEntries(),
                doctor,
                request.getTimezone(),
                request.getNotes()
        );

        List<DoctorSchedule> savedSchedules = doctorScheduleRepository.saveAll(newSchedules);

        log.info("Successfully updated schedule for doctor: {}", doctorId);

        return doctorScheduleMapper.toDoctorScheduleResponse(doctor, savedSchedules);
    }


    @Override
    @Transactional
    public void deleteDoctorSchedule(UUID doctorId) {
        findAndValidateDoctor(doctorId);

        int deactivatedCount = doctorScheduleRepository.deactivateSchedulesByDoctorId(doctorId);

        if (deactivatedCount == 0) {
            log.warn("No schedule found to delete for doctor: {}", doctorId);
            throw new CustomException(ErrorCode.SCHEDULE_NOT_FOUND);
        }

        log.info("Successfully deactivated {} schedule entries for doctor: {}", deactivatedCount, doctorId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DoctorSearchResponse> searchDoctors(DoctorSearchRequest request) {
        Specification<User> specification = specificationService.buildDoctorSearchSpecification(request);

        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                Sort.Direction.fromString(request.getSortDirection()),
                request.getSortBy()
        );

        Page<User> doctorPage = userRepository.findAll(specification, pageable);

        PageResponse<DoctorSearchResponse> response = pageMapper.toPageResponse(doctorPage, doctor -> {
            List<DoctorSchedule> schedules = doctorScheduleRepository.findByDoctorIdAndIsActiveTrue(doctor.getId());
            return doctorSearchMapper.toDoctorSearchResponse(doctor, schedules);
        });

        log.debug("Found {} doctors matching search criteria", response.getTotalElements());

        return response;
    }

    private User findAndValidateDoctor(UUID doctorId) {
        User doctor = userRepository.findById(doctorId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!isDoctorRole(doctor)) {
            throw new CustomException(ErrorCode.INSUFFICIENT_PERMISSION);
        }

        return doctor;
    }

    private boolean isDoctorRole(User user) {
        return user.getUserRoles().stream()
                .anyMatch(userRole -> "DOCTOR".equals(userRole.getRole().getName()));
    }
}
