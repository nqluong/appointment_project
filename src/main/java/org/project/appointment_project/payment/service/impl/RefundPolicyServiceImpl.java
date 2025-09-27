package org.project.appointment_project.payment.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.project.appointment_project.common.exception.CustomException;
import org.project.appointment_project.common.exception.ErrorCode;
import org.project.appointment_project.payment.dto.request.PaymentRefundRequest;
import org.project.appointment_project.payment.enums.RefundType;
import org.project.appointment_project.payment.model.Payment;
import org.project.appointment_project.payment.service.RefundPolicyService;
import org.springframework.stereotype.Service;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RefundPolicyServiceImpl implements RefundPolicyService {

    private static final int FULL_REFUND_DAYS = 2;
    private static final BigDecimal FULL_REFUND_PERCENTAGE = BigDecimal.valueOf(1.00); // 100%
    private static final BigDecimal PARTIAL_REFUND_PERCENTAGE = BigDecimal.valueOf(0.30); // 30%

    @Override
    public BigDecimal calculateRefundAmount(Payment payment, PaymentRefundRequest request) {
        if (payment == null || request == null) {
            throw new IllegalArgumentException("Thông tin thanh toán và yêu cầu hoàn tiền không được để trống");
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
                    "Cần phải nhập số tiền hoàn trả cho loại hoàn tiền tùy chỉnh");
        }

        validateCustomRefundAmount(payment.getAmount(), customAmount);
        return customAmount;
    }

    private void validateCustomRefundAmount(BigDecimal originalAmount, BigDecimal customAmount) {
        if (customAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException(ErrorCode.INVALID_REFUND_AMOUNT,
                    "Số tiền hoàn trả phải lớn hơn 0");
        }

        if (customAmount.compareTo(originalAmount) > 0) {
            throw new CustomException(ErrorCode.INVALID_REFUND_AMOUNT,
                    "Số tiền hoàn trả không thể vượt quá số tiền thanh toán ban đầu");
        }
    }

    private BigDecimal calculatePolicyBasedRefund(Payment payment) {
        LocalDateTime cancellationTime = LocalDateTime.now();
        LocalDate appointmentDate = payment.getAppointment().getAppointmentDate();

        if (appointmentDate == null) {
            throw new IllegalArgumentException("Ngày hẹn không được để trống");
        }

        BigDecimal refundPercentage = calculateRefundPercentage(appointmentDate, cancellationTime);
        BigDecimal refundAmount = payment.getAmount()
                .multiply(refundPercentage)
                .setScale(2, RoundingMode.HALF_UP);

        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException(ErrorCode.INVALID_REFUND_AMOUNT,
                    "Số tiền hoàn trả tính toán phải lớn hơn 0");
        }

        return refundAmount;
    }

    @Override
    public BigDecimal calculateRefundPercentage(LocalDate appointmentDate, LocalDateTime cancellationDateTime) {
        if (appointmentDate == null || cancellationDateTime == null) {
            throw new IllegalArgumentException("Ngày hẹn và ngày hủy không được để trống");
        }

        long daysUntilAppointment = ChronoUnit.DAYS.between(cancellationDateTime.toLocalDate(), appointmentDate);

        return daysUntilAppointment >= FULL_REFUND_DAYS
                ? FULL_REFUND_PERCENTAGE
                : PARTIAL_REFUND_PERCENTAGE;
    }
}