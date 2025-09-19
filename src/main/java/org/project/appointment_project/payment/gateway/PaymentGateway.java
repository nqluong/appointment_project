package org.project.appointment_project.payment.gateway;

import org.project.appointment_project.payment.dto.request.PaymentCallbackRequest;
import org.project.appointment_project.payment.enums.PaymentMethod;
import org.project.appointment_project.payment.gateway.dto.PaymentGatewayRequest;
import org.project.appointment_project.payment.gateway.dto.PaymentGatewayResponse;
import org.project.appointment_project.payment.gateway.dto.PaymentVerificationResult;
import org.project.appointment_project.payment.model.Payment;

public interface PaymentGateway {

    PaymentGatewayResponse createPaymentUrl(Payment payment, PaymentGatewayRequest request);

    PaymentVerificationResult verifyPayment(PaymentCallbackRequest callbackRequest);

    boolean supports(PaymentMethod paymentMethod);
}
