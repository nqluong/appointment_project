package org.project.appointment_project.payment.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.payment.service.PaymentService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentSchedulerService {

    PaymentService paymentService;

    @Scheduled(fixedRate = 300000) //5 phut
    @Async
    public void processExpiredPayments(){
        try {
            log.debug("Starting scheduled task: processExpiredPayments");
            paymentService.processExpiredPayments();
            log.debug("Completed scheduled task: processExpiredPayments");
        } catch (Exception e) {
            log.error("Error in scheduled task processExpiredPayments: {}", e.getMessage(), e);
        }
    }
}
