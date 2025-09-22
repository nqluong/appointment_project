package org.project.appointment_project.payment.service;

import org.project.appointment_project.appoinment.model.Appointment;
import org.project.appointment_project.payment.enums.PaymentType;

import java.math.BigDecimal;

public interface PaymentAmountCalculator {
    BigDecimal calculatePaymentAmount(Appointment appointment, PaymentType paymentType);
}
