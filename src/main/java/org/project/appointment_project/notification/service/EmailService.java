package org.project.appointment_project.notification.service;

import org.project.appointment_project.notification.dto.request.PasswordResetNotificationRequest;
import org.project.appointment_project.notification.dto.request.PaymentNotificationRequest;

public interface EmailService {

    boolean sendPaymentSuccessEmail(PaymentNotificationRequest notificationDto);

    boolean sendPasswordResetEmail(PasswordResetNotificationRequest notificationDto);

}
