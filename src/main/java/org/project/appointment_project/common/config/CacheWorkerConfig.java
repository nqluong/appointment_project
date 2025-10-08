package org.project.appointment_project.common.config;

import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.common.redis.RedisCacheService;
import org.project.appointment_project.process.DoctorAvailabilityCacheProcess;
import org.project.appointment_project.schedule.repository.DoctorAvailableSlotRepository;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Configuration
@EnableScheduling
public class CacheWorkerConfig {
    private static final int WORKER_THREAD_COUNT = 3;

    @Bean(destroyMethod = "shutdown")
    public ExecutorService cacheWorkerThreadPool(
            RedisCacheService redisCacheService,
            DoctorAvailableSlotRepository slotRepository) {

        ExecutorService executorService = Executors.newFixedThreadPool(WORKER_THREAD_COUNT);

        for (int i = 0; i < WORKER_THREAD_COUNT; i++) {
            DoctorAvailabilityCacheProcess worker =
                    new DoctorAvailabilityCacheProcess(redisCacheService, slotRepository);

            executorService.submit(worker);
        }

        log.info("Cache hoạt động với {} threads", WORKER_THREAD_COUNT);
        return executorService;
    }
}
