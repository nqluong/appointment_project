package org.project.appointment_project.process;

import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.common.redis.RedisCacheService;
import org.project.appointment_project.schedule.dto.cache.DoctorAvailabilityCacheData;
import org.project.appointment_project.schedule.dto.cache.TimeSlot;
import org.project.appointment_project.schedule.model.DoctorAvailableSlot;
import org.project.appointment_project.schedule.repository.DoctorAvailableSlotRepository;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class DoctorAvailabilityCacheProcess implements Runnable {

    private final RedisCacheService redisCacheService;
    private final DoctorAvailableSlotRepository slotRepository;
    private final String QUEUE_KEY = "doctor_availability_cache_queue";
    private final String CACHE_PREFIX = "doctor:availability:";
    private final int CACHE_TTL = 1; // 1
    private final int DAYS_TO_CACHE = 7;
    private volatile boolean running = true;

    public DoctorAvailabilityCacheProcess(RedisCacheService redisCacheService,
                                          DoctorAvailableSlotRepository slotRepository) {
        this.redisCacheService = redisCacheService;
        this.slotRepository = slotRepository;
    }

    @Override
    public void run() {
        try {
            while (running) {
                try {
                    Object doctorIdObj = redisCacheService.rightPop(QUEUE_KEY);

                    if (doctorIdObj != null) {
                        processDoctorAvailability(doctorIdObj.toString());
                    } else {
                        Thread.sleep(500);
                    }
                } catch (InterruptedException e) {
                    log.warn("Thread bị gián đoạn, dừng tiến trình.");
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("Lỗi khi xử lý lịch khám: {}",
                          e.getMessage());
                    Thread.sleep(1000);
                }
            }
        } catch(Exception e){
            log.error("Lỗi nghiêm trọng: {}",  e.getMessage());
        }

        log.info("DoctorAvailabilityCacheProcess dừng - Thread: {}", Thread.currentThread().getName());
    }

    private void processDoctorAvailability(String doctorIdStr) {
        UUID doctorId = UUID.fromString(doctorIdStr);

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(DAYS_TO_CACHE);

        List<DoctorAvailableSlot> slots = slotRepository
                .findSlotsByDoctorAndDateRange(doctorId, startDate, endDate);

        if (slots.isEmpty()) {
            return;
        }

        Map<LocalDate, List<DoctorAvailableSlot>> slotsByDate = slots.stream()
                .collect(Collectors.groupingBy(DoctorAvailableSlot::getSlotDate));


        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            List<DoctorAvailableSlot> dailySlots = slotsByDate.getOrDefault(
                    currentDate, Collections.emptyList());

            cacheDailySlots(doctorId, currentDate, dailySlots);

            currentDate = currentDate.plusDays(1);
        }
    }

    private void cacheDailySlots(UUID doctorId, LocalDate date,
                                 List<DoctorAvailableSlot> dailySlots) {
        List<TimeSlot> slots = dailySlots.stream()
                .map(this::convertToTimeSlot)
                .collect(Collectors.toList());


        DoctorAvailabilityCacheData cacheData = DoctorAvailabilityCacheData.builder()
                .doctorId(doctorId)
                .date(date.toString())
                .slots(slots)
                .totalSlots(dailySlots.size())
                .build();

        String cacheKey = CACHE_PREFIX + doctorId + ":" + date;
        redisCacheService.set(cacheKey, cacheData, CACHE_TTL, TimeUnit.DAYS);
    }


    private TimeSlot convertToTimeSlot(DoctorAvailableSlot slot) {
        return TimeSlot.builder()
                .slotId(slot.getId())
                .startTime(slot.getStartTime().toString())
                .endTime(slot.getEndTime().toString())
                .isAvailable(slot.isAvailable())
                .build();
    }

    public void stop() {
        running = false;
    }
}
