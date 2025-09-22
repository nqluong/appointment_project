package org.project.appointment_project.payment.service;

import org.project.appointment_project.payment.dto.request.CreatePaymentRequest;
import org.project.appointment_project.payment.dto.request.PaymentCallbackRequest;
import org.project.appointment_project.payment.dto.response.PaymentResponse;
import org.project.appointment_project.payment.dto.response.PaymentUrlResponse;

import java.util.UUID;

public interface PaymentService {

    PaymentUrlResponse createPayment(CreatePaymentRequest request, String customerIp);

    PaymentResponse processPaymentCallback(PaymentCallbackRequest callbackRequest);

    PaymentResponse getPaymentById(UUID paymentId);

    PaymentResponse cancelPayment(UUID paymentId);

    void handlePaymentSuccess(UUID paymentId);

    void handlePaymentFailure(UUID paymentId);

    void processExpiredPayments();

    PaymentResponse queryPaymentStatus(UUID paymentId);

    PaymentResponse queryPaymentStatus(String transactionId);

    void processProcessingPayments();
}
