package org.project.appointment_project.payment.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.appoinment.repository.AppointmentRepository;
import org.project.appointment_project.common.exception.CustomException;
import org.project.appointment_project.common.exception.ErrorCode;
import org.project.appointment_project.payment.dto.request.CreatePaymentRequest;
import org.project.appointment_project.payment.enums.PaymentStatus;
import org.project.appointment_project.payment.model.Payment;
import org.project.appointment_project.payment.repository.PaymentRepository;
import org.project.appointment_project.payment.service.PaymentValidationService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentValidationServiceImpl implements PaymentValidationService {
    PaymentRepository paymentRepository;
    AppointmentRepository appointmentRepository;

    @Override
    public void validateCreatePaymentRequest(CreatePaymentRequest request) {
        if(request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0){
            throw new CustomException(ErrorCode.PAYMENT_INVALID_AMOUNT);
        }

        if (!appointmentRepository.existsById(request.getAppointmentId())) {
            throw new CustomException(ErrorCode.PAYMENT_NOT_FOUND, "Appointment not found");
        }

        boolean hasPendingPayment = paymentRepository.existsByAppointmentIdAndPaymentStatusIn(
                request.getAppointmentId(),
                Arrays.asList(PaymentStatus.PENDING, PaymentStatus.PROCESSING)
        );

        if (hasPendingPayment) {
            throw new CustomException(ErrorCode.PAYMENT_ALREADY_PROCESSED,
                    "There is already a pending payment for this appointment");
        }
    }

    @Override
    public void validatePaymentCancellation(Payment payment) {
        List<PaymentStatus> cancellableStatuses = Arrays.asList(
                PaymentStatus.PENDING,
                PaymentStatus.PROCESSING
        );

        if (!cancellableStatuses.contains(payment.getPaymentStatus())) {
            throw new CustomException(ErrorCode.PAYMENT_INVALID_STATUS,
                    "Cannot cancel payment with status: " + payment.getPaymentStatus());
        }
    }

    @Override
    public void validatePaymentStatusTransition(Payment payment, PaymentStatus newStatus) {
        PaymentStatus currentStatus = payment.getPaymentStatus();

        // Logic chuyen doi trang thai payment
        boolean isValidTransition = switch (currentStatus) {
            case PENDING -> Arrays.asList(PaymentStatus.PROCESSING, PaymentStatus.CANCELLED, PaymentStatus.FAILED)
                    .contains(newStatus);
            case PROCESSING -> Arrays.asList(PaymentStatus.COMPLETED, PaymentStatus.FAILED, PaymentStatus.CANCELLED)
                    .contains(newStatus);
            case COMPLETED -> PaymentStatus.REFUNDED.equals(newStatus);
            case FAILED, CANCELLED, REFUNDED -> false;
        };

        if (!isValidTransition) {
            throw new CustomException(ErrorCode.PAYMENT_INVALID_STATUS,
                    String.format("Invalid status transition from %s to %s", currentStatus, newStatus));
        }
    }
}
