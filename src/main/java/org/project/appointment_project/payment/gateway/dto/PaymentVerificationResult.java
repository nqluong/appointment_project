package org.project.appointment_project.payment.gateway.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.project.appointment_project.payment.enums.PaymentStatus;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentVerificationResult {
    boolean valid;
    String transactionId;
    String gatewayTransactionId;
    BigDecimal amount;
    PaymentStatus status;
    String message;
    String responseData;
}
