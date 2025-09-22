package org.project.appointment_project.payment.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.appoinment.enums.Status;
import org.project.appointment_project.appoinment.model.Appointment;
import org.project.appointment_project.appoinment.repository.AppointmentRepository;
import org.project.appointment_project.common.exception.CustomException;
import org.project.appointment_project.common.exception.ErrorCode;
import org.project.appointment_project.payment.enums.PaymentType;
import org.project.appointment_project.payment.model.Payment;
import org.project.appointment_project.payment.repository.PaymentRepository;
import org.project.appointment_project.payment.service.PaymentStatusHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentStatusHandlerImpl implements PaymentStatusHandler {

    PaymentRepository paymentRepository;
    AppointmentRepository appointmentRepository;

    @Override
    public void handlePaymentSuccess(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        if (payment.getPaymentType() == PaymentType.DEPOSIT) {
            Appointment appointment = payment.getAppointment();
            appointment.setStatus(Status.CONFIRMED);
            appointmentRepository.save(appointment);

            log.info("Appointment {} confirmed after successful deposit payment {}",
                    appointment.getId(), paymentId);
        }
    }

    @Override
    public void handlePaymentFailure(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        log.info("Payment {} failed, appointment {} remains PENDING",
                paymentId, payment.getAppointment().getId());
    }
}
