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

    static final int EXPIRATION_MINUTES = 15;

    AppointmentRepository appointmentRepository;
    PaymentRepository paymentRepository;
    SlotStatusService slotStatusService;

    //Tìm và xử lý các appointment đã ở trạng thái chờ quá 15 phút
    @Override
    public void processExpiredAppointments() {
        LocalDateTime expiredTime = LocalDateTime.now().minusMinutes(EXPIRATION_MINUTES);
        List<Appointment> expiredAppointments = findExpiredAppointments(expiredTime);

        log.info("Found {} expired appointments to process", expiredAppointments.size());

        for (Appointment appointment : expiredAppointments) {
            processExpiredAppointment(appointment.getId());
        }
    }

    // Xử lý appointment đã hết hạn
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processExpiredAppointment(UUID appointmentId) {
        try {
            Appointment appointment = findAppointmentById(appointmentId);

            if (!isEligibleForExpiration(appointment)) {
                log.debug("Appointment {} không đủ điều kiện để xử lý hết hạn", appointmentId);
                return;
            }

            executeExpirationProcess(appointment);
            log.info("Đã xử lý thành công appointment hết hạn: {}", appointmentId);

        } catch (Exception e) {
            log.error("Lỗi khi xử lý appointment hết hạn {}: {}", appointmentId, e.getMessage(), e);
            throw new RuntimeException("Không thể xử lý appointment hết hạn: " + appointmentId, e);
        }
    }

    // Tìm các appointment đã hết hạn dựa trên thời gian
    private List<Appointment> findExpiredAppointments(LocalDateTime expirationThreshold) {
        return appointmentRepository.findExpiredPendingAppointments(expirationThreshold);
    }

    private Appointment findAppointmentById(UUID appointmentId) {
        return appointmentRepository.findById(appointmentId).orElse(null);
    }

    private boolean isEligibleForExpiration(Appointment appointment) {
        if (appointment == null) {
            return false;
        }

        if (appointment.getStatus() != Status.PENDING) {
            return false;
        }

        return appointment.getCreatedAt()
                .isBefore(LocalDateTime.now().minusMinutes(EXPIRATION_MINUTES));
    }

    // Hủy appointment, hủy payment, giải phóng slot
    private void executeExpirationProcess(Appointment appointment) {
        cancelAppointment(appointment);
        cancelAssociatedPayments(appointment.getId());
        releaseAssociatedSlot(appointment);
    }

    // Hủy appointment
    private void cancelAppointment(Appointment appointment) {
        appointment.setStatus(Status.CANCELLED);
        appointmentRepository.save(appointment);
        log.debug("Đã hủy appointment: {}", appointment.getId());
    }

    // Hủy những payment ở pending và processing
    private void cancelAssociatedPayments(UUID appointmentId) {
        List<PaymentStatus> cancellableStatuses = Arrays.asList(
                PaymentStatus.PENDING,
                PaymentStatus.PROCESSING
        );

        List<Payment> paymentsToCancel = paymentRepository
                .findValidPaymentsByAppointmentIdAndStatus(appointmentId, cancellableStatuses);

        paymentsToCancel.forEach(this::cancelPayment);

        log.debug("Đã hủy {} payment cho appointment: {}",
                paymentsToCancel.size(), appointmentId);
    }

    // Hủy appointment cụ thể
    private void cancelPayment(Payment payment) {
        payment.setPaymentStatus(PaymentStatus.CANCELLED);
        payment.setNotes("Hủy do appointment hết hạn");
        paymentRepository.save(payment);
    }

    //Giải phóng các slot liên quan tới appointment
    private void releaseAssociatedSlot(Appointment appointment) {
        if (appointment.getSlot() != null) {
            try {
                slotStatusService.releaseSlot(appointment.getSlot().getId());
                log.debug("Đã giải phóng slot: {}", appointment.getSlot().getId());
            } catch (Exception e) {
                log.warn("Không thể giải phóng slot {}: {}",
                        appointment.getSlot().getId(), e.getMessage());
            }
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
