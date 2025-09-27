package org.project.appointment_project.medicalrecord.service.impl;

import org.project.appointment_project.appoinment.enums.Status;
import org.project.appointment_project.medicalrecord.dto.response.MedicalRecordResponse;
import org.project.appointment_project.medicalrecord.repository.MedicalRecordProjection;
import org.springframework.stereotype.Service;

@Service
public class ProjectionMapperService {

    public MedicalRecordResponse toMedicalRecordResponse(MedicalRecordProjection projection) {
        if (projection == null) {
            return null;
        }

        return MedicalRecordResponse.builder()
                // Medical Record fields
                .id(projection.getId())
                .diagnosis(projection.getDiagnosis())
                .prescription(projection.getPrescription())
                .testResults(projection.getTestResults())
                .followUpNotes(projection.getFollowUpNotes())
                .doctorNotes(projection.getDoctorNotes())
                .createdAt(projection.getCreatedAt())
                .updatedAt(projection.getUpdatedAt())

                // Appointment fields
                .appointmentId(projection.getAppointmentId())
                .appointmentDate(projection.getAppointmentDate())
                .appointmentTime(projection.getAppointmentTime())
                .appointmentStatus(projection.getAppointmentStatus())
                .consultationFee(projection.getConsultationFee())
                .appointmentReason(projection.getAppointmentReason())
                .appointmentNotes(projection.getAppointmentNotes())

                // Doctor fields - already flattened from native query
                .doctorId(projection.getDoctorId())
                .doctorName(projection.getDoctorName())
                .doctorEmail(projection.getDoctorEmail())
                .doctorSpecialty(projection.getDoctorSpecialty())
                .doctorSpecialtyCode(projection.getDoctorSpecialtyCode())
                .doctorLicenseNumber(projection.getDoctorLicenseNumber())
                .doctorQualification(projection.getDoctorQualification())
                .doctorYearsOfExperience(projection.getDoctorYearsOfExperience())
                .doctorBio(projection.getDoctorBio())

                // Patient fields - already flattened from native query
                .patientId(projection.getPatientId())
                .patientName(projection.getPatientName())
                .patientEmail(projection.getPatientEmail())
                .patientPhone(projection.getPatientPhone())
                .patientDateOfBirth(projection.getPatientDateOfBirth())
                .patientGender(projection.getPatientGender())
                .patientBloodType(projection.getPatientBloodType())
                .patientAllergies(projection.getPatientAllergies())
                .patientMedicalHistory(projection.getPatientMedicalHistory())
                .patientEmergencyContactName(projection.getPatientEmergencyContactName())
                .patientEmergencyContactPhone(projection.getPatientEmergencyContactPhone())

                // Metadata
                .createdBy(projection.getCreatedBy())
                .lastUpdatedBy(projection.getLastUpdatedBy())
                .build();
    }

    private Status parseStatus(String statusString) {
        if (statusString == null) {
            return null;
        }
        try {
            return Status.valueOf(statusString.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Log warning và return default value
            System.err.println("Invalid status value: " + statusString);
            return null;
        }
    }

    private String buildFullName(String firstName, String lastName) {
        if (firstName == null && lastName == null) {
            return null;
        }
        if (firstName == null) {
            return lastName;
        }
        if (lastName == null) {
            return firstName;
        }
        return firstName + " " + lastName;
    }
}
