package org.project.appointment_project.appoinment.service;

import org.project.appointment_project.schedule.model.DoctorAvailableSlot;
import org.project.appointment_project.user.model.User;

import java.util.UUID;

public interface AppointmentValidationService {
    void validateSlotForBooking(DoctorAvailableSlot slot, UUID doctorId);

    void validatePatientForBooking(User patient, DoctorAvailableSlot slot);

    void validateDoctorForBooking(User doctor);
}
