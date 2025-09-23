package org.project.appointment_project.payment.service.impl;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.project.appointment_project.common.exception.CustomException;
import org.project.appointment_project.common.exception.ErrorCode;
import org.project.appointment_project.payment.dto.request.PaymentRefundRequest;
import org.project.appointment_project.payment.enums.RefundType;
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

    private static final int FULL_REFUND_DAYS = 2;
    private static final BigDecimal FULL_REFUND_PERCENTAGE = BigDecimal.valueOf(1.00); // 100%
    private static final BigDecimal PARTIAL_REFUND_PERCENTAGE = BigDecimal.valueOf(0.30); // 30%

    @Override
    public BigDecimal calculateRefundAmount(Payment payment, PaymentRefundRequest request) {
        if (payment == null || request == null) {
            throw new IllegalArgumentException("Payment and refund request cannot be null");
        }

        RefundType refundType = request.getRefundType() != null ? request.getRefundType() : RefundType.POLICY_BASED;

        return switch (refundType) {
            case FULL_REFUND -> calculateFullRefund(payment);
            case CUSTOM_AMOUNT -> calculateCustomRefund(payment, request);
            case POLICY_BASED -> calculatePolicyBasedRefund(payment);
        };
    }

    private BigDecimal calculateFullRefund(Payment payment) {
        return payment.getAmount();
    }

    private BigDecimal calculateCustomRefund(Payment payment, PaymentRefundRequest request) {
        BigDecimal customAmount = request.getCustomRefundAmount();
        if (customAmount == null) {
            throw new CustomException(ErrorCode.INVALID_REFUND_AMOUNT,
                    "Custom refund amount is required for CUSTOM_AMOUNT type");
        }

        validateCustomRefundAmount(payment.getAmount(), customAmount);
        return customAmount;
    }

    private void validateCustomRefundAmount(BigDecimal originalAmount, BigDecimal customAmount) {
        if (customAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException(ErrorCode.INVALID_REFUND_AMOUNT,
                    "Custom refund amount must be greater than 0");
        }

        if (customAmount.compareTo(originalAmount) > 0) {
            throw new CustomException(ErrorCode.INVALID_REFUND_AMOUNT,
                    "Custom refund amount cannot exceed original payment amount");
        }
    }

    private BigDecimal calculatePolicyBasedRefund(Payment payment) {
        LocalDateTime cancellationTime = LocalDateTime.now();
        LocalDate appointmentDate = payment.getAppointment().getAppointmentDate();

        if (appointmentDate == null) {
            throw new IllegalArgumentException("Appointment date cannot be null");
        }

        BigDecimal refundPercentage = calculateRefundPercentage(appointmentDate, cancellationTime);
        BigDecimal refundAmount = payment.getAmount()
                .multiply(refundPercentage)
                .setScale(2, RoundingMode.HALF_UP);

        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException(ErrorCode.INVALID_REFUND_AMOUNT,
                    "Calculated refund amount must be greater than 0");
        }

        return refundAmount;
    }

    @Override
    public BigDecimal calculateRefundPercentage(LocalDate appointmentDate, LocalDateTime cancellationDateTime) {
        if (appointmentDate == null || cancellationDateTime == null) {
            throw new IllegalArgumentException("Appointment and cancellation dates cannot be null");
        }

        long daysUntilAppointment = ChronoUnit.DAYS.between(cancellationDateTime.toLocalDate(), appointmentDate);

        return daysUntilAppointment >= FULL_REFUND_DAYS
                ? FULL_REFUND_PERCENTAGE
                : PARTIAL_REFUND_PERCENTAGE;
    }
}