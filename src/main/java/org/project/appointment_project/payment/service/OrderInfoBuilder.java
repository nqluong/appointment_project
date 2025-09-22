package org.project.appointment_project.payment.service;

import org.project.appointment_project.appoinment.model.Appointment;
import org.project.appointment_project.payment.enums.PaymentType;

import java.util.UUID;

public interface OrderInfoBuilder {
    String buildOrderInfo(PaymentType paymentType, UUID appointmentId);
}
