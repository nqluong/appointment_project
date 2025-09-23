package org.project.appointment_project.payment.service.impl;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.project.appointment_project.payment.model.Payment;
import org.project.appointment_project.payment.service.RefundPolicyService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RefundPolicyServiceImpl implements RefundPolicyService {

    static final int FULL_REFUND_DAYS = 2;
    static final BigDecimal FULL_REFUND_PERCENTAGE = BigDecimal.valueOf(1.00); // 100%
    static final BigDecimal PARTIAL_REFUND_PERCENTAGE = BigDecimal.valueOf(0.30); // 30%

    @Override
    public BigDecimal calculateRefundPercentage(LocalDate appointmentDate, LocalDateTime cancellationDateTime) {
        if (appointmentDate == null || cancellationDateTime == null) {
            throw new IllegalArgumentException("Appointment and cancellation dates cannot be null");
        }

        long daysUntilAppointment = ChronoUnit.DAYS.between(cancellationDateTime.toLocalDate(), appointmentDate);

        if (daysUntilAppointment >= FULL_REFUND_DAYS) {
            return FULL_REFUND_PERCENTAGE; // 100%
        } else {
            return PARTIAL_REFUND_PERCENTAGE; // 70%
        }
    }

    @Override
    public BigDecimal calculateRefundAmount(Payment payment, BigDecimal refundPercentage) {


        // Tính số tiền hoàn theo phần trăm
        BigDecimal calculatedRefund = payment.getAmount().multiply(refundPercentage)
                .setScale(2, RoundingMode.HALF_UP);

        return calculatedRefund;

    }
}
