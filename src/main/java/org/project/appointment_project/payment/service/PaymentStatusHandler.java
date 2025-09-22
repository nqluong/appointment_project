package org.project.appointment_project.payment.service;

import java.util.UUID;

public interface PaymentStatusHandler {

    void handlePaymentSuccess(UUID paymentId);

    void handlePaymentFailure(UUID paymentId);
}
