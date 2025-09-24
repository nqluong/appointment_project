package org.project.appointment_project.notification.service;

import org.project.appointment_project.payment.model.Payment;

public interface NotificationService {
    void sendPaymentSuccessNotification(Payment payment);
}
