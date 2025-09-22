package org.project.appointment_project.payment.service;

import java.util.UUID;

public interface AppointmentExpirationService {

    void processExpiredAppointments();

    void processExpiredAppointment(UUID appointmentId);

}
