package org.project.appointment_project.payment.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.appoinment.enums.Status;
import org.project.appointment_project.appoinment.model.Appointment;
import org.project.appointment_project.appoinment.repository.AppointmentRepository;
import org.project.appointment_project.payment.enums.PaymentStatus;
import org.project.appointment_project.payment.model.Payment;
import org.project.appointment_project.payment.repository.PaymentRepository;
import org.project.appointment_project.payment.service.AppointmentExpirationService;
import org.project.appointment_project.schedule.service.SlotStatusService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AppointmentExpirationServiceImpl implements AppointmentExpirationService {

    AppointmentRepository appointmentRepository;
    PaymentRepository paymentRepository;
    SlotStatusService slotStatusService;

    @Override
    public void processExpiredAppointments() {
        LocalDateTime expiredTime = LocalDateTime.now().minusMinutes(15);
        List<Appointment> expiredAppointments = appointmentRepository
                .findExpiredPendingAppointments(expiredTime);

        log.info("Found {} expired appointments to process", expiredAppointments.size());

        for (Appointment appointment : expiredAppointments) {
            processExpiredAppointment(appointment.getId());
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processExpiredAppointment(UUID appointmentId) {
        try {
            Appointment appointment = appointmentRepository.findById(appointmentId)
                    .orElse(null);

            if (!isAppointmentExpired(appointment)) {
                return;
            }

            cancelExpiredAppointment(appointment);
            cancelRelatedPayments(appointmentId);
            releaseSlot(appointment);

            log.info("Successfully processed expired appointment {} with payments",
                    appointmentId);

        } catch (Exception e) {
            log.error("Error processing expired appointment {}: {}", appointmentId, e.getMessage());
            throw new RuntimeException("Failed to process expired appointment: " + appointmentId);
        }
    }

    private boolean isAppointmentExpired(Appointment appointment) {
        if (appointment == null || appointment.getStatus() != Status.PENDING) {
            return false;
        }

        return appointment.getCreatedAt().isBefore(LocalDateTime.now().minusMinutes(15));
    }

    private void cancelExpiredAppointment(Appointment appointment) {
        appointment.setStatus(Status.CANCELLED);
        appointmentRepository.save(appointment);
    }

    private void cancelRelatedPayments(UUID appointmentId) {
        List<Payment> pendingPayments = paymentRepository
                .findValidPaymentsByAppointmentIdAndStatus(
                        appointmentId,
                        Arrays.asList(PaymentStatus.PENDING, PaymentStatus.PROCESSING));

        for (Payment payment : pendingPayments) {
            payment.setPaymentStatus(PaymentStatus.CANCELLED);
            payment.setNotes("Cancelled due to expired appointment");
            paymentRepository.save(payment);
        }
    }

    private void releaseSlot(Appointment appointment) {
        if (appointment.getSlot() != null) {
            slotStatusService.releaseSlot(appointment.getSlot().getId());
        }
    }
}
