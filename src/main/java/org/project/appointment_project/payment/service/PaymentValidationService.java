package org.project.appointment_project.payment.service;

import org.project.appointment_project.payment.dto.request.CreatePaymentRequest;
import org.project.appointment_project.payment.enums.PaymentStatus;
import org.project.appointment_project.payment.model.Payment;

public interface PaymentValidationService {

    void validateCreatePaymentRequest(CreatePaymentRequest request);

    void validatePaymentCancellation(Payment payment);

    void validatePaymentStatusTransition(Payment payment, PaymentStatus newStatus);
}
