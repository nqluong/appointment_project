package org.project.appointment_project.payment.gateway.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentGatewayRequest {
    String returnUrl;
    String cancelUrl;
    String orderInfo;
    String customerIp;
}
