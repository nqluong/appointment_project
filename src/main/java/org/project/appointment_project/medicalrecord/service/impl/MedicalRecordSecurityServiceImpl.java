package org.project.appointment_project.medicalrecord.service.impl;

import java.util.List;
import java.util.UUID;

import org.project.appointment_project.appoinment.model.Appointment;
import org.project.appointment_project.common.exception.CustomException;
import org.project.appointment_project.common.exception.ErrorCode;
import org.project.appointment_project.common.util.SecurityUtils;
import org.project.appointment_project.medicalrecord.service.MedicalRecordSecurityService;
import org.project.appointment_project.user.model.User;
import org.project.appointment_project.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MedicalRecordSecurityServiceImpl implements MedicalRecordSecurityService {

    UserRepository userRepository;
    SecurityUtils securityUtils;

    @Override
    public User getCurrentUser() {
        UUID currentUserId = securityUtils.getCurrentUserId();
        return userRepository.findById(currentUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    @Override
    public void validateApprovedDoctor() {
        if (!securityUtils.hasRole("DOCTOR")) {
            UUID currentUserId = securityUtils.getCurrentUserId();
            log.warn("Người dùng {} không có vai trò BÁC SĨ", currentUserId);
            throw new CustomException(ErrorCode.DOCTOR_PERMISSION_REQUIRED);
        }

        User user = getCurrentUser();

        boolean isDoctor = user.getUserRoles().stream()
                .anyMatch(role -> "DOCTOR".equals(role.getRole().getName()));

        if (!isDoctor) {
            throw new CustomException(ErrorCode.DOCTOR_PERMISSION_REQUIRED);
        }

        if (user.getMedicalProfile() == null || !user.getMedicalProfile().isDoctorApproved()) {
            throw new CustomException(ErrorCode.DOCTOR_NOT_APPROVED);
        }
    }

    @Override
    public void validateViewAccess(Appointment appointment) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        List<String> userRoles = securityUtils.getCurrentUserRoles();

        // Admin có thể xem tất cả
        if (userRoles.contains("ADMIN")) {
            return;
        }

        // Doctor có thể xem hồ sơ của bệnh nhân trong cuộc hẹn của họ
        if (userRoles.contains("DOCTOR") && appointment.getDoctor().getId().equals(currentUserId)) {
            return;
        }

        // Patient có thể xem hồ sơ của chính họ
        if (userRoles.contains("PATIENT") && appointment.getPatient().getId().equals(currentUserId)) {
            return;
        }

        log.warn("Từ chối quyền xem cho người dùng {} đối với cuộc hẹn {}", currentUserId, appointment.getId());
        throw new CustomException(ErrorCode.UNAUTHORIZED_MEDICAL_RECORD_ACCESS);
    }

    @Override
    public void validateUpdateAccess(Appointment appointment) {
        UUID currentUserId = securityUtils.getCurrentUserId();

        // Chỉ doctor người tạo mới được update
        if (!appointment.getDoctor().getId().equals(currentUserId)) {
            log.warn("Người dùng {} không có quyền cập nhật hồ sơ bệnh án của cuộc hẹn {}",
                    currentUserId, appointment.getId());
            throw new CustomException(ErrorCode.UNAUTHORIZED_MEDICAL_RECORD_UPDATE);
        }
    }

    @Override
    public void validateCreateAccess(Appointment appointment) {
        UUID currentUserId = securityUtils.getCurrentUserId();

    
        validateApprovedDoctor();

        // Validate appointment có phải của doctor hiện tại
        if (!appointment.getDoctor().getId().equals(currentUserId)) {
            log.warn("Cuộc hẹn {} không thuộc về bác sĩ hiện tại {}",
                    appointment.getId(), currentUserId);
            throw new CustomException(ErrorCode.UNAUTHORIZED_MEDICAL_RECORD_ACCESS);
        }

    }

    @Override
    public void validateSummaryAccess(boolean allowPatients) {
        List<String> roles = securityUtils.getCurrentUserRoles();
        UUID currentUserId = securityUtils.getCurrentUserId();

        if (roles.contains("ADMIN") || roles.contains("DOCTOR")) {
            return;
        }

        if (allowPatients && roles.contains("PATIENT")) {
            return;
        }

        log.warn("Từ chối quyền truy cập tóm tắt cho người dùng {} với các vai trò {}", currentUserId, roles);
        throw new CustomException(ErrorCode.ACCESS_DENIED);
    }

    @Override
    public UUID applyUserBasedFiltering(UUID targetUserId, String parameterType) {
        if (securityUtils.isCurrentUserAdmin()) {
            return targetUserId;
        }

        UUID currentUserId = securityUtils.getCurrentUserId();
        List<String> roles = securityUtils.getCurrentUserRoles();

        if (roles.contains("DOCTOR") && "doctorId".equals(parameterType)) {
            log.debug("Lọc doctorId thành người dùng hiện tại {} cho vai trò bác sĩ", currentUserId);
            return currentUserId; // Doctors có thể xem các bản ghi của chính họ
        } else if (roles.contains("PATIENT") && "patientId".equals(parameterType)) {
            log.debug("Lọc patientId thành người dùng hiện tại {} cho vai trò bệnh nhân", currentUserId);
            return currentUserId; // Patients có thể xem các bản ghi của chính họ
        }

        return targetUserId; // For other cases, return original value
    }

    @Override
    public boolean canAccessSummaries() {
        List<String> roles = securityUtils.getCurrentUserRoles();
        return roles.contains("ADMIN") || roles.contains("DOCTOR");
    }
}
