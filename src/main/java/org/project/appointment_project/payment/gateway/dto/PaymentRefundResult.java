package org.project.appointment_project.payment.gateway.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.project.appointment_project.payment.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentRefundResult {
    boolean success;
    String refundTransactionId;
    String gatewayRefundId;
    BigDecimal refundAmount;
    PaymentStatus status;
    String responseCode;
    String message;
    LocalDateTime refundDate;
    String rawResponse;
    String errorCode;
}
