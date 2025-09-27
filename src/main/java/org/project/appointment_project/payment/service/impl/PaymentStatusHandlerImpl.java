package org.project.appointment_project.payment.service.impl;

import java.util.UUID;

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

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

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

        Appointment appointment = payment.getAppointment();

        if (payment.getPaymentType() == PaymentType.DEPOSIT) {
            appointment.setStatus(Status.CONFIRMED);
            appointmentRepository.save(appointment);

            log.info("Lịch hẹn {} đã được xác nhận sau khi thanh toán đặt cọc {} thành công",
                    appointment.getId(), paymentId);

        } else if (payment.getPaymentType() == PaymentType.FULL) {
            appointment.setStatus(Status.CONFIRMED);
            appointmentRepository.save(appointment);

            log.info("Lịch hẹn {} đã được xác nhận sau khi thanh toán toàn bộ {} thành công",
                    appointment.getId(), paymentId);

        }
    }

    @Override
    public void handlePaymentFailure(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        log.info("Thanh toán {} thất bại, lịch hẹn {} vẫn ở trạng thái CHỜ XỬ LÝ",
                paymentId, payment.getAppointment().getId());
    }
}
