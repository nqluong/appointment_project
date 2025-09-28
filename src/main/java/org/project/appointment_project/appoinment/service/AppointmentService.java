package org.project.appointment_project.appoinment.service;

import org.project.appointment_project.appoinment.dto.request.CreateAppointmentRequest;
import org.project.appointment_project.appoinment.dto.response.AppointmentResponse;
import org.project.appointment_project.appoinment.enums.Status;
import org.project.appointment_project.common.dto.PageResponse;
import org.project.appointment_project.medicalrecord.dto.request.CreateMedicalRecordRequest;
import org.project.appointment_project.medicalrecord.dto.request.UpdateMedicalRecordRequest;
import org.project.appointment_project.medicalrecord.dto.response.MedicalRecordResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AppointmentService {

    AppointmentResponse createAppointment(CreateAppointmentRequest request);

    PageResponse<AppointmentResponse> getAppointments(UUID userId, Status status, Pageable pageable);

    AppointmentResponse updateAppointmentStatus(UUID appointmentId, Status newStatus);

    AppointmentResponse completeAppointment(UUID appointmentId);

    AppointmentResponse cancelAppointment(UUID appointmentId, String reason);

    AppointmentResponse startExamination(UUID appointmentId);

    MedicalRecordResponse completeAppointmentWithMedicalRecord(CreateMedicalRecordRequest request);

    MedicalRecordResponse updateMedicalRecordForAppointment(UUID appointmentId, UpdateMedicalRecordRequest request);
}
