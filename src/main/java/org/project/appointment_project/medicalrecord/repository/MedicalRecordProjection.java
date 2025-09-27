package org.project.appointment_project.medicalrecord.repository;

import org.project.appointment_project.appoinment.enums.Status;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public interface MedicalRecordProjection {
    UUID getId();

    // Appointment information
    UUID getAppointmentId();
    LocalDate getAppointmentDate();
    LocalTime getAppointmentTime();
    Status getAppointmentStatus();
    BigDecimal getConsultationFee();
    String getAppointmentReason();
    String getAppointmentNotes();
    String getDoctorNotes();

    // Doctor information
    UUID getDoctorId();
    String getDoctorName();
    String getDoctorEmail();
    String getDoctorSpecialty();

    UUID getDoctorSpecialtyCode();
    String getDoctorLicenseNumber();
    String getDoctorQualification();
    Integer getDoctorYearsOfExperience();
    String getDoctorBio();

    // Patient information
    UUID getPatientId();
    String getPatientName();
    String getPatientEmail();
    String getPatientPhone();
    LocalDate getPatientDateOfBirth();
    String getPatientGender();
    String getPatientBloodType();
    String getPatientAllergies();
    String getPatientMedicalHistory();
    String getPatientEmergencyContactName();
    String getPatientEmergencyContactPhone();

    String getDiagnosis();
    String getPrescription();
    String getTestResults();
    String getFollowUpNotes();

    LocalDateTime getCreatedAt();
    LocalDateTime getUpdatedAt();

    String getCreatedBy();
    String getLastUpdatedBy();
}
