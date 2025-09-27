package org.project.appointment_project.medicalrecord.service;

import org.project.appointment_project.appoinment.model.Appointment;
import org.project.appointment_project.user.model.User;

import java.util.UUID;

public interface MedicalRecordSecurityService {
    User getCurrentUser();

    void validateApprovedDoctor();

    void validateViewAccess(Appointment appointment);

    void validateUpdateAccess(Appointment appointment);

    void validateCreateAccess(Appointment appointment);

    // Xác thực người dùng hiện tại có thể truy cập danh sách tóm tắt hồ sơ bệnh án
    void validateSummaryAccess(boolean allowPatients);

    UUID applyUserBasedFiltering(UUID targetUserId, String parameterType);

    boolean canAccessSummaries();
}
