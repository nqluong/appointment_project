package org.project.appointment_project.payment.service;

import org.project.appointment_project.payment.dto.response.PaymentResponse;

import java.util.UUID;

public interface PaymentQueryService {

    PaymentResponse queryPaymentStatus(UUID paymentId);

    PaymentResponse queryPaymentStatus(String transactionId);

    void processProcessingPayments();
}
