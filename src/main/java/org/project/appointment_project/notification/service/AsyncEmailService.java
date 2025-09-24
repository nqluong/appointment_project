package org.project.appointment_project.notification.service;

import org.springframework.scheduling.annotation.Async;

public interface AsyncEmailService {
    void sendPasswordResetEmailAsync(
            String email,
            String userName,
            String resetToken,
            String resetUrl,
            int expiryTime);
}
