package org.project.appointment_project.schedule.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.common.dto.PageResponse;
import org.project.appointment_project.common.exception.CustomException;
import org.project.appointment_project.common.exception.ErrorCode;
import org.project.appointment_project.common.mapper.PageMapper;
import org.project.appointment_project.common.redis.RedisCacheService;
import org.project.appointment_project.schedule.dto.cache.DoctorAvailabilityCacheData;
import org.project.appointment_project.schedule.dto.cache.TimeSlot;
import org.project.appointment_project.schedule.dto.response.AvailableSlotInfo;
import org.project.appointment_project.schedule.dto.response.DoctorWithSlotsResponse;
import org.project.appointment_project.schedule.mapper.DoctorAvailabilityMapper;
import org.project.appointment_project.schedule.repository.DoctorWithSlotsProjection;
import org.project.appointment_project.schedule.repository.DoctorWithSlotsRepository;
import org.project.appointment_project.schedule.repository.SlotProjection;
import org.project.appointment_project.schedule.service.DoctorAvailabilityService;
import org.project.appointment_project.user.dto.response.DoctorResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.*;
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
    RedisCacheService redisCacheService;

    private static final String PROFILE_CACHE_PREFIX = "doctor:profile:";
    private static final String AVAILABILITY_CACHE_PREFIX = "doctor:availability:";


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
                    List<AvailableSlotInfo> slotInfos = getLimitedSlotsWithCache(
                            projection.getUserId(), startDate, endDate, 3);


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

    @Override
    public DoctorWithSlotsResponse getDoctorAvailableSlots1(UUID doctorId, LocalDate startDate, LocalDate endDate) {
        DoctorWithSlotsProjection doctorProjection = repository.findDoctorById(doctorId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        DoctorWithSlotsResponse response = doctorAvailabilityMapper.toBaseDoctorResponse(doctorProjection);

        List<SlotProjection> slots = repository.findAllAvailableSlotsByDoctorId(doctorId, startDate, endDate);

        List<AvailableSlotInfo> slotInfos = slots.stream()
                .map(doctorAvailabilityMapper::toAvailableSlotInfo)
                .collect(Collectors.toList());

        response.setAvailableSlots(slotInfos);

        return response;
    }

    @Override
    public DoctorWithSlotsResponse getDoctorAvailableSlots2(UUID doctorId, LocalDate startDate, LocalDate endDate) {
        log.info("Lấy slots của bác sĩ {} từ {} đến {}", doctorId, startDate, endDate);

        // Lấy thông tin bác sĩ từ cache hoặc DB
        DoctorWithSlotsResponse response = getDoctorInfoWithCache(doctorId);

        // Lấy tất cả slots từ cache hoặc DB
        List<AvailableSlotInfo> slotInfos = getAllSlotsInRangeWithCache(
                doctorId, startDate, endDate);

        response.setAvailableSlots(slotInfos);

        log.info("Bác sĩ {} có {} slots từ {} đến {}",
                doctorId, slotInfos.size(), startDate, endDate);

        return response;
    }

    // Cache key: doctor:profile:{doctorId}
    private DoctorWithSlotsResponse getDoctorInfoWithCache(UUID doctorId) {
        String cacheKey = PROFILE_CACHE_PREFIX + doctorId;

        try {
            // Kiểm tra cache profile
            Object cachedProfile = redisCacheService.get(cacheKey);

            if (cachedProfile instanceof DoctorResponse) {
                log.info("Cache HIT - Profile bác sĩ {}", doctorId);
                DoctorResponse doctorResponse = (DoctorResponse) cachedProfile;

                return convertDoctorResponseToWithSlots(doctorResponse);
            }

            log.info("Cache MISS - Profile bác sĩ {}", doctorId);

        } catch (Exception e) {
            log.warn("Lỗi đọc cache profile bác sĩ {}: {}", doctorId, e.getMessage());
        }

        // Cache miss, Query DB
        log.info("Truy vấn DB lấy profile bác sĩ {}", doctorId);
        DoctorWithSlotsProjection projection = repository.findDoctorById(doctorId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return doctorAvailabilityMapper.toBaseDoctorResponse(projection);
    }

    /**
     * Lấy slots giới hạn với cache
     */
    private List<AvailableSlotInfo> getLimitedSlotsWithCache(
            UUID doctorId,
            LocalDate startDate,
            LocalDate endDate,
            int limit) {

        List<AvailableSlotInfo> allSlots = new ArrayList<>();
        LocalDate currentDate = startDate;
        int count = 0;

        // Lấy slots từng ngày cho đến khi đủ limit hoặc hết range
        while (!currentDate.isAfter(endDate) && count < limit) {
            List<AvailableSlotInfo> dailySlots = getDailySlotsWithCache(
                    doctorId, currentDate);

            for (AvailableSlotInfo slot : dailySlots) {
                if (count >= limit) break;
                allSlots.add(slot);
                count++;
            }

            currentDate = currentDate.plusDays(1);
        }

        return allSlots;
    }

    // Lấy tất cả slots trong khoảng với cache
    private List<AvailableSlotInfo> getAllSlotsInRangeWithCache(
            UUID doctorId,
            LocalDate startDate,
            LocalDate endDate) {

        List<AvailableSlotInfo> allSlots = new ArrayList<>();
        LocalDate currentDate = startDate;

        // Lấy slots từng ngày
        while (!currentDate.isAfter(endDate)) {
            List<AvailableSlotInfo> dailySlots = getDailySlotsWithCache(
                    doctorId, currentDate);

            allSlots.addAll(dailySlots);
            currentDate = currentDate.plusDays(1);
        }

        return allSlots;
    }


    /**
     * Cache key: doctor:availability:{doctorId}:{date}
     */
    private List<AvailableSlotInfo> getDailySlotsWithCache(UUID doctorId, LocalDate date) {
        String cacheKey = AVAILABILITY_CACHE_PREFIX + doctorId + ":" + date;

        try {
            if (redisCacheService.exists(cacheKey)) {
                Object cachedData = redisCacheService.get(cacheKey);

                if (cachedData instanceof DoctorAvailabilityCacheData) {
                    DoctorAvailabilityCacheData cacheData = (DoctorAvailabilityCacheData) cachedData;

                    // Kiểm tra xem có phải ngày rỗng không
                    if (cacheData.getTotalSlots() != null && cacheData.getTotalSlots() == 0) {
                        log.info("Cache HIT (EMPTY) - Bác sĩ {} không có slot ngày {}",
                                doctorId, date);
                        return Collections.emptyList();
                    }

                    log.info("Cache HIT - Bác sĩ {} ngày {} có {} available slots",
                            doctorId, date, cacheData.getSlots().size());

                    return convertCacheDataToSlotInfoList(cacheData);
                }
            }

            // Cache không tồn tại → Ngoài range 7 ngày của worker
            log.info("Cache MISS - Slots bác sĩ {} ngày {} (ngoài range cache)",
                    doctorId, date);

        } catch (Exception e) {
            log.warn("Lỗi đọc cache slots bác sĩ {} ngày {}: {}",
                    doctorId, date, e.getMessage());
        }

        // Cache miss → Query DB và cache động
        return getDailySlotsFromDatabase(doctorId, date);
    }

    private List<AvailableSlotInfo> getDailySlotsFromDatabase(UUID doctorId, LocalDate date) {
        List<SlotProjection> slots = repository.findAvailableSlotsByDoctorIdAndDate(
                doctorId, date, date);

        return slots.stream()
                .map(doctorAvailabilityMapper::toAvailableSlotInfo)
                .collect(Collectors.toList());
    }


    private List<AvailableSlotInfo> convertCacheDataToSlotInfoList(
            DoctorAvailabilityCacheData cacheData) {

        if (cacheData.getSlots() == null) {
            return Collections.emptyList();
        }

        LocalDate date;
        try {
            date = LocalDate.parse(cacheData.getDate());
        } catch (DateTimeParseException e) {
            log.error("Lỗi parse date từ cache: {}", cacheData.getDate());
            return Collections.emptyList();
        }

        return cacheData.getSlots().stream()
                .map(timeSlot -> convertTimeSlotToAvailableSlotInfo(timeSlot, date))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private AvailableSlotInfo convertTimeSlotToAvailableSlotInfo(
            TimeSlot timeSlot,
            LocalDate date) {

        try {
            LocalTime startTime = LocalTime.parse(timeSlot.getStartTime());
            LocalTime endTime = LocalTime.parse(timeSlot.getEndTime());

            return AvailableSlotInfo.builder()
                    .slotId(String.valueOf(timeSlot.getSlotId()))
                    .slotDate(date)
                    .startTime(startTime)
                    .endTime(endTime)
                    .isAvailable(timeSlot.isAvailable())
                    .build();

        } catch (DateTimeParseException e) {
            log.error("Lỗi parse time từ cache: {} - {}",
                    timeSlot.getStartTime(), timeSlot.getEndTime());
            return null;
        }
    }


    private DoctorWithSlotsResponse convertDoctorResponseToWithSlots(DoctorResponse doctorResponse) {
        return DoctorWithSlotsResponse.builder()
                .userId(String.valueOf(doctorResponse.getId()))
                .fullName(doctorResponse.getFullName())
                .avatarUrl(doctorResponse.getAvatarUrl())
                .qualification(doctorResponse.getQualification())
                .consultationFee(doctorResponse.getConsultationFee())
                .yearsOfExperience(doctorResponse.getYearsOfExperience())
                .gender(doctorResponse.getGender())
                .phone(doctorResponse.getPhone())
                .specialtyName(doctorResponse.getSpecialtyName())
                .availableSlots(new ArrayList<>())
                .build();
    }

}
