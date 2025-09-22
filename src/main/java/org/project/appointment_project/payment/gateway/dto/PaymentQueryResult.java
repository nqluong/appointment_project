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
public class PaymentQueryResult {
    boolean success;
    PaymentStatus status;
    String transactionId;
    String gatewayTransactionId;
    BigDecimal amount;
    String responseCode;
    String message;
    LocalDateTime paymentDate;
    String rawResponse;

}
