package org.project.appointment_project.payment.gateway;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.project.appointment_project.common.exception.CustomException;
import org.project.appointment_project.common.exception.ErrorCode;
import org.project.appointment_project.payment.enums.PaymentMethod;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentGatewayFactory {

    List<PaymentGateway> paymentGateways;

    public PaymentGateway getGateway(PaymentMethod paymentMethod) {
        return paymentGateways.stream()
                .filter(gateway -> gateway.supports(paymentMethod))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_PROCESSING_FAILED));
    }
}
