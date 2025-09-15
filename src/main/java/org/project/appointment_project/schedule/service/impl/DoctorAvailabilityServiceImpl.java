package org.project.appointment_project.schedule.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.common.dto.PageResponse;
import org.project.appointment_project.common.mapper.PageMapper;
import org.project.appointment_project.schedule.dto.response.AvailableSlotInfo;
import org.project.appointment_project.schedule.dto.response.DoctorWithSlotsResponse;
import org.project.appointment_project.schedule.mapper.DoctorAvailabilityMapper;
import org.project.appointment_project.schedule.repository.DoctorWithSlotsProjection;
import org.project.appointment_project.schedule.repository.DoctorWithSlotsRepository;
import org.project.appointment_project.schedule.repository.SlotProjection;
import org.project.appointment_project.schedule.service.DoctorAvailabilityService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DoctorAvailabilityServiceImpl implements DoctorAvailabilityService {

    DoctorWithSlotsRepository repository;
    DoctorAvailabilityMapper doctorAvailabilityMapper;
    PageMapper pageMapper;

    @Override
    public PageResponse<DoctorWithSlotsResponse> getDoctorsWithAvailableSlots(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        Page<DoctorWithSlotsProjection> projectionPage = repository
                .findDoctorsWithAvailableSlots(startDate, endDate, pageable);

        return processProjectionsToResponse(projectionPage, startDate, endDate, pageable);
    }

    @Override
    public PageResponse<DoctorWithSlotsResponse> getDoctorsWithAvailableSlotsBySpecialty(UUID specialtyId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        Page<DoctorWithSlotsProjection> projectionPage = repository
                .findDoctorsWithAvailableSlotsBySpecialty(specialtyId, startDate, endDate, pageable);

        return processProjectionsToResponse(projectionPage, startDate, endDate, pageable);
    }

    private PageResponse<DoctorWithSlotsResponse> processProjectionsToResponse(
            Page<DoctorWithSlotsProjection> projectionPage,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable) {

        List<DoctorWithSlotsResponse> responses = projectionPage.getContent().stream()
                .map(projection -> {
                    DoctorWithSlotsResponse response = doctorAvailabilityMapper.toBaseDoctorResponse(projection);

                    // Lấy slots riêng biệt cho mỗi doctor với limit 3
                    List<SlotProjection> slots = repository.findAvailableSlotsByDoctorId(
                            projection.getUserId(), startDate, endDate);

                    List<AvailableSlotInfo> slotInfos = slots.stream()
                            .map(doctorAvailabilityMapper::toAvailableSlotInfo)
                            .collect(Collectors.toList());

                    response.setAvailableSlots(slotInfos);

                    log.debug("Doctor {} has {} available slots",
                            response.getUserId(), slotInfos.size());

                    return response;
                })
                .collect(Collectors.toList());

        Page<DoctorWithSlotsResponse> responsePage = new PageImpl<>(
                responses, pageable, projectionPage.getTotalElements());

        return pageMapper.toPageResponse(responsePage, response -> response);
    }
}
