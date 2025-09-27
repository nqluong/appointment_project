package org.project.appointment_project.medicalrecord.service;

import org.project.appointment_project.appoinment.model.Appointment;
import org.project.appointment_project.user.model.User;

import java.util.UUID;

public interface MedicalRecordValidationService {

    // Xác thực trạng thái cuộc hẹn hợp lệ cho các thao tác với hồ sơ bệnh án
    void validateAppointmentStatus(Appointment appointment);

    // Xác thực không tồn tại hồ sơ bệnh án cho cuộc hẹn
    void validateNoExistingMedicalRecord(UUID appointmentId);

    // Xác thực  cho việc tạo hồ sơ bệnh án
    void validateMedicalRecordCreation(Appointment appointment);

    // Xác thực cuộc hẹn và quyền người dùng để tạo hồ sơ bệnh án
    void validateAppointmentForMedicalRecord(Appointment appointment, UUID currentUserId);

    void validateDoctorPermission(User user);

    void validateViewPermission(Appointment appointment);

    void validateUpdatePermission(Appointment appointment);

    void validateCreatePermission();

    void validateAppointmentOwnership(Appointment appointment);

    void validateSummaryAccess(boolean allowPatients);

    UUID applyUserBasedFiltering(UUID targetUserId, String parameterType);
}
