package org.project.appointment_project.medicalrecord.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.appoinment.enums.Status;
import org.project.appointment_project.appoinment.model.Appointment;
import org.project.appointment_project.common.exception.CustomException;
import org.project.appointment_project.common.exception.ErrorCode;
import org.project.appointment_project.medicalrecord.repository.MedicalRecordRepository;
import org.project.appointment_project.medicalrecord.service.MedicalRecordSecurityService;
import org.project.appointment_project.medicalrecord.service.MedicalRecordValidationService;
import org.project.appointment_project.user.model.User;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MedicalRecordValidationServiceImpl implements MedicalRecordValidationService {

    MedicalRecordRepository medicalRecordRepository;
    MedicalRecordSecurityService securityService;

    @Override
    public void validateAppointmentStatus(Appointment appointment) {
        if (appointment.getStatus() != Status.IN_PROGRESS && appointment.getStatus() != Status.COMPLETED) {
            throw new CustomException(ErrorCode.INVALID_APPOINTMENT_STATUS_FOR_MEDICAL_RECORD);
        }
    }

    @Override
    public void validateNoExistingMedicalRecord(UUID appointmentId) {
        if (medicalRecordRepository.existsByAppointmentId(appointmentId)) {
            throw new CustomException(ErrorCode.MEDICAL_RECORD_ALREADY_EXISTS);
        }
    }

    @Override
    public void validateMedicalRecordCreation(Appointment appointment) {
        validateAppointmentStatus(appointment);
        validateNoExistingMedicalRecord(appointment.getId());
        securityService.validateCreateAccess(appointment);
    }

    @Override
    public void validateAppointmentForMedicalRecord(Appointment appointment, UUID currentUserId) {
        validateAppointmentStatus(appointment);

        if (!appointment.getDoctor().getId().equals(currentUserId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_MEDICAL_RECORD_ACCESS);
        }
    }

    @Override
    public void validateDoctorPermission(User user) {
        securityService.validateApprovedDoctor();
    }

    @Override
    public void validateViewPermission(Appointment appointment) {
        securityService.validateViewAccess(appointment);
    }

    @Override
    public void validateUpdatePermission(Appointment appointment) {
        securityService.validateUpdateAccess(appointment);
    }

    @Override
    public void validateCreatePermission() {
        securityService.validateApprovedDoctor();
    }

    @Override
    public void validateAppointmentOwnership(Appointment appointment) {
        securityService.validateCreateAccess(appointment);
    }

    @Override
    public void validateSummaryAccess(boolean allowPatients) {
        securityService.validateSummaryAccess(allowPatients);
    }

    @Override
    public UUID applyUserBasedFiltering(UUID targetUserId, String parameterType) {
        return securityService.applyUserBasedFiltering(targetUserId, parameterType);
    }
}
