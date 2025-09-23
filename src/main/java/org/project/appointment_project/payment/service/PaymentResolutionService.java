package org.project.appointment_project.payment.service;

import org.project.appointment_project.payment.model.Payment;

import java.util.UUID;

public interface PaymentResolutionService {
    Payment resolvePayment(UUID paymentId, UUID appointmentId);
}
