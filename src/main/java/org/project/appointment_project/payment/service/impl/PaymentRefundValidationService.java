package org.project.appointment_project.payment.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.project.appointment_project.common.exception.CustomException;
import org.project.appointment_project.common.exception.ErrorCode;
import org.project.appointment_project.payment.dto.request.PaymentRefundRequest;
import org.project.appointment_project.payment.model.Payment;
import org.project.appointment_project.payment.util.PaymentRefundUtil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentRefundValidationService {

    PaymentRefundUtil paymentRefundUtil;

    public void validateRefundRequest(PaymentRefundRequest request) {
        if (request.getPaymentId() == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST, "Payment ID is required");
        }
    }

    public void validatePaymentForRefund(Payment payment, PaymentRefundRequest request) {
        // Check payment status
        if (!paymentRefundUtil.isRefundable(payment)) {
            throw new CustomException(ErrorCode.PAYMENT_NOT_REFUNDABLE,
                    "Only completed payments can be refunded");
        }

        // Check if already fully refunded
        if (paymentRefundUtil.isFullyRefunded(payment)) {
            throw new CustomException(ErrorCode.PAYMENT_ALREADY_REFUNDED,
                    "Payment has been fully refunded");
        }

        // Check refund period (ví dụ: 30 ngày)
        if (payment.getPaymentDate() != null &&
                payment.getPaymentDate().isBefore(LocalDateTime.now().minusDays(30))) {
            throw new CustomException(ErrorCode.REFUND_PERIOD_EXPIRED,
                    "Refund period has expired");
        }

    }
}
