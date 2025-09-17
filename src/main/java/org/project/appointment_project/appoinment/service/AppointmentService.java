package org.project.appointment_project.appoinment.service;

import org.project.appointment_project.appoinment.dto.request.CreateAppointmentRequest;
import org.project.appointment_project.appoinment.dto.response.AppointmentResponse;

public interface AppointmentService {

    AppointmentResponse createAppointment(CreateAppointmentRequest request);
}
