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
import org.project.appointment_project.schedule.dto.cache.CacheKeyInfo;
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

        List<UUID> doctorIds = projectionPage.getContent().stream()
                .map(DoctorWithSlotsProjection::getUserId)
                .toList();

        Map<UUID, List<AvailableSlotInfo>> slotsMap =
                loadLimitedSlotsFromCache(doctorIds, startDate, endDate, 3);

        List<DoctorWithSlotsResponse> responses = projectionPage.getContent().stream()
                .map(projection -> {
                    DoctorWithSlotsResponse response =
                            doctorAvailabilityMapper.toBaseDoctorResponse(projection);

                    // Slots từ cache
                    List<AvailableSlotInfo> slots = slotsMap.getOrDefault(
                            projection.getUserId(), Collections.emptyList());
                    response.setAvailableSlots(slots);

                    log.debug("Doctor {} có {} slots từ cache",
                            projection.getUserId(), slots.size());

                    return response;
                })
                .collect(Collectors.toList());

        Page<DoctorWithSlotsResponse> responsePage = new PageImpl<>(
                responses, pageable, projectionPage.getTotalElements());

        return pageMapper.toPageResponse(responsePage, response -> response);
    }

    @Override
    public DoctorWithSlotsResponse getDoctorAvailableSlots(UUID doctorId, LocalDate startDate, LocalDate endDate) {
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
    public DoctorWithSlotsResponse getDoctorAvailableSlotsCache(UUID doctorId, LocalDate startDate, LocalDate endDate) {
        log.info("Lấy slots của bác sĩ {} từ {} đến {}", doctorId, startDate, endDate);

        // Lấy thông tin bác sĩ từ cache hoặc DB
        DoctorWithSlotsResponse response = getDoctorInfoWithCache(doctorId);

        // Lấy tất cả slots từ cache hoặc DB
        List<AvailableSlotInfo> slotInfos = getAllSlotsInRangeWithCache(
                doctorId, startDate, endDate);

        response.setAvailableSlots(slotInfos);

        return response;
    }

    private Map<UUID, List<AvailableSlotInfo>> loadLimitedSlotsFromCache(
            List<UUID> doctorIds,
            LocalDate startDate,
            LocalDate endDate,
            int limitPerDoctor) {

        Map<UUID, List<AvailableSlotInfo>> resultMap = new HashMap<>();

        List<String> allCacheKeys = new ArrayList<>();
        Map<String, CacheKeyInfo> keyInfoMap = new HashMap<>();

        for (UUID doctorId : doctorIds) {
            LocalDate currentDate = startDate;
            while (!currentDate.isAfter(endDate)) {
                String cacheKey = AVAILABILITY_CACHE_PREFIX + doctorId + ":" + currentDate;
                allCacheKeys.add(cacheKey);
                keyInfoMap.put(cacheKey, new CacheKeyInfo(doctorId, currentDate));
                currentDate = currentDate.plusDays(1);
            }
        }

        // get tất cả slots cùng lúc
        Map<UUID, List<AvailableSlotInfo>> tempSlotsMap = new HashMap<>();

        try {
            List<Object> cachedValues = redisCacheService.mget(allCacheKeys);

            for (int i = 0; i < allCacheKeys.size(); i++) {
                String cacheKey = allCacheKeys.get(i);
                Object cached = cachedValues.get(i);
                CacheKeyInfo keyInfo = keyInfoMap.get(cacheKey);

                if (cached instanceof DoctorAvailabilityCacheData) {
                    DoctorAvailabilityCacheData cacheData = (DoctorAvailabilityCacheData) cached;

                    if (cacheData.getTotalSlots() != null && cacheData.getTotalSlots() == 0) {
                        continue;
                    }

                    List<AvailableSlotInfo> dailySlots = convertCacheDataToSlotInfoList(cacheData);

                    tempSlotsMap.computeIfAbsent(keyInfo.getDoctorId(), k -> new ArrayList<>())
                            .addAll(dailySlots);
                }
            }


        } catch (Exception e) {
            log.error("Lỗi get slots từ cache: {}", e.getMessage());
        }

        for (UUID doctorId : doctorIds) {
            List<AvailableSlotInfo> allSlots = tempSlotsMap.getOrDefault(
                    doctorId, new ArrayList<>());

            List<AvailableSlotInfo> limitedSlots = allSlots.stream()
                    .limit(limitPerDoctor)
                    .collect(Collectors.toList());

            resultMap.put(doctorId, limitedSlots);
        }

        return resultMap;
    }


    // Cache key: doctor:profile:{doctorId}
    private DoctorWithSlotsResponse getDoctorInfoWithCache(UUID doctorId) {
        String cacheKey = PROFILE_CACHE_PREFIX + doctorId;

        try {

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

    // Lấy tất cả slots trong khoảng với cache
    private List<AvailableSlotInfo> getAllSlotsInRangeWithCache(
            UUID doctorId,
            LocalDate startDate,
            LocalDate endDate) {

        List<AvailableSlotInfo> allSlots = new ArrayList<>();
        List<LocalDate> cacheMissDates = new ArrayList<>();

        List<String> cacheKeys = new ArrayList<>();
        List<LocalDate> dates = new ArrayList<>();

        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            cacheKeys.add(AVAILABILITY_CACHE_PREFIX + doctorId + ":" + currentDate);
            dates.add(currentDate);
            currentDate = currentDate.plusDays(1);
        }

        try {
            List<Object> cachedValues = redisCacheService.mget(cacheKeys);

            for (int i = 0; i < dates.size(); i++) {
                LocalDate date = dates.get(i);
                Object cached = cachedValues.get(i);

                if (cached instanceof DoctorAvailabilityCacheData) {
                    DoctorAvailabilityCacheData cacheData = (DoctorAvailabilityCacheData) cached;

                    if (cacheData.getTotalSlots() != null && cacheData.getTotalSlots() == 0) {
                        continue;
                    }

                    List<AvailableSlotInfo> dailySlots = convertCacheDataToSlotInfoList(cacheData);
                    allSlots.addAll(dailySlots);

                } else {
                    cacheMissDates.add(date);
                }
            }

            log.info("Slots bác sĩ {}: Cache HIT {}/{} ngày",
                    doctorId, (dates.size() - cacheMissDates.size()), dates.size());

        } catch (Exception e) {
            cacheMissDates.addAll(dates);
        }

        //  cho các ngày cache miss
        if (!cacheMissDates.isEmpty()) {
            log.info("Load {} ngày từ DB cho bác sĩ {} (ngoài range cache)",
                    cacheMissDates.size(), doctorId);

            for (LocalDate date : cacheMissDates) {
                List<SlotProjection> dbSlots = repository.findAvailableSlotsByDoctorIdAndDate(
                        doctorId, date, date);

                List<AvailableSlotInfo> dailySlots = dbSlots.stream()
                        .map(doctorAvailabilityMapper::toAvailableSlotInfo)
                        .collect(Collectors.toList());

                allSlots.addAll(dailySlots);
            }
        }

        return allSlots;
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
