package org.project.appointment_project.payment.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RefundCalculationResult {
    BigDecimal refundAmount;
    BigDecimal refundPercentage;
}
