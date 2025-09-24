package org.project.appointment_project.auth.service;

import org.project.appointment_project.auth.dto.request.ForgotPasswordRequest;
import org.project.appointment_project.auth.dto.request.PasswordResetRequest;
import org.project.appointment_project.auth.dto.response.ForgotPasswordResponse;
import org.project.appointment_project.auth.dto.response.PasswordResetResponse;

public interface PasswordResetService {

    ForgotPasswordResponse forgotPassword(ForgotPasswordRequest forgotPasswordRequest);

    PasswordResetResponse passwordReset(PasswordResetRequest passwordResetRequest);

    boolean validateResetToken(String token);

}
