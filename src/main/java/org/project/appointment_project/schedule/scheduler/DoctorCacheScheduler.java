package org.project.appointment_project.schedule.scheduler;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.common.redis.RedisCacheService;
import org.project.appointment_project.user.dto.response.DoctorResponse;
import org.project.appointment_project.user.mapper.DoctorMapper;
import org.project.appointment_project.user.model.User;
import org.project.appointment_project.user.repository.DoctorRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;


@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DoctorCacheScheduler {

    DoctorRepository doctorRepository;
    DoctorMapper doctorMapper;
    RedisCacheService redisCacheService;

    private static final String AVAILABILITY_QUEUE_KEY = "doctor_availability_cache_queue";
    private static final long PROFILE_CACHE_TTL = 7;
    private static final String PROFILE_CACHE_PREFIX = "doctor:profile:";

    private static final int PROFILE_PAGE_SIZE = 20;
    private static final int MAX_PROFILE_PAGES = 2;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        try {
            cacheDoctorProfiles();
            cleanupExpiredSlots();
        } catch (Exception ex) {
            log.error("Lỗi trong khởi động cache bác sĩ: {}", ex.getMessage(), ex);
        }
    }

    @Async("    taskExecutor")
    @Scheduled(cron = "0 0 0,6,12,18 * * * ")
    public void cacheDoctorProfiles() {
        try {
            List<User> doctors = fetchAllApprovedDoctors();

            for (User doctor : doctors) {
                DoctorResponse doctorResponse = doctorMapper.toResponse(doctor);
                String cacheKey = PROFILE_CACHE_PREFIX + doctor.getId();

                redisCacheService.set(cacheKey, doctorResponse, PROFILE_CACHE_TTL, TimeUnit.DAYS);
                redisCacheService.leftPush(AVAILABILITY_QUEUE_KEY, doctor.getId().toString());
            }

            log.info("Cached {} doctor profiles", doctors.size());

        } catch (Exception ex) {
            log.error("Lỗi trong job cache profile bác sĩ: {}", ex.getMessage(), ex);
        }
    }

    @Async("taskExecutor")
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupExpiredSlots() {
        try {
            LocalDate today = LocalDate.now();
            String basePattern = "doctor:availability:*";

            // Lấy tất cả keys theo pattern
            Set<String> allKeys = redisCacheService.keys(basePattern);

            if (allKeys == null || allKeys.isEmpty()) {
                log.info("Không có keys nào để cleanup");
                return;
            }

            List<String> expiredKeys = new ArrayList<>();
            for (String key : allKeys) {
                try {
                    //doctor:availability:{doctorId}:{date}
                    String[] parts = key.split(":");
                    if (parts.length >= 4) {
                        String dateStr = parts[3];
                        LocalDate slotDate = LocalDate.parse(dateStr);

                        if (slotDate.isBefore(today)) {
                            expiredKeys.add(key);
                        }
                    }


                } catch (Exception e) {
                    log.warn("Không thể parse key: {} - {}", key, e.getMessage());
                }
            }

            if (!expiredKeys.isEmpty()) {
                long deletedCount = redisCacheService.delete(expiredKeys);
                log.info("Đã cleanup {} expired slots từ {} keys", deletedCount, expiredKeys.size());
            }
        } catch (Exception ex) {
            log.error("Lỗi trong job cleanup: {}", ex.getMessage(), ex);
        }
    }

    private List<User> fetchAllApprovedDoctors() {
        List<User> allDoctors = new ArrayList<>();

        for (int pageNumber = 0; pageNumber < MAX_PROFILE_PAGES; pageNumber++) {
            Pageable pageable = PageRequest.of(pageNumber, PROFILE_PAGE_SIZE);
            Page<User> doctorPage = doctorRepository.findAllApprovedDoctors2(pageable);

            if (doctorPage.isEmpty()) {
                break;
            }

            allDoctors.addAll(doctorPage.getContent());

            if (doctorPage.isLast()) {
                break;
            }
        }

        return allDoctors;
    }

}
