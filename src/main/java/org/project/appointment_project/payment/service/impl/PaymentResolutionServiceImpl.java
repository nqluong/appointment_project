package org.project.appointment_project.payment.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.appoinment.model.Appointment;
import org.project.appointment_project.appoinment.repository.AppointmentRepository;
import org.project.appointment_project.common.exception.CustomException;
import org.project.appointment_project.common.exception.ErrorCode;
import org.project.appointment_project.payment.enums.PaymentStatus;
import org.project.appointment_project.payment.model.Payment;
import org.project.appointment_project.payment.repository.PaymentRepository;
import org.project.appointment_project.payment.service.PaymentResolutionService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentResolutionServiceImpl implements PaymentResolutionService {

    PaymentRepository paymentRepository;
    AppointmentRepository appointmentRepository;

    @Override
    public Payment resolvePayment(UUID paymentId, UUID appointmentId) {
        if (paymentId != null) {
            return paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));
        }

        if (appointmentId != null) {
            Appointment appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new CustomException(ErrorCode.APPOINTMENT_NOT_FOUND));

            boolean hasRefundedPayment = paymentRepository.existsByAppointmentAndPaymentStatus(
                    appointment, PaymentStatus.REFUNDED);
            if (hasRefundedPayment) {
                throw new CustomException(ErrorCode.PAYMENT_ALREADY_REFUNDED,
                        "This appointment already has a refunded payment");
            }
            return paymentRepository.findByAppointmentAndPaymentStatus(appointment, PaymentStatus.COMPLETED)
                    .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND,
                            "No completed payment found for appointment"));
        }

        throw new CustomException(ErrorCode.INVALID_REQUEST,
                "Either paymentId or appointmentId must be provided");
    }

}
