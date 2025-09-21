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
import org.project.appointment_project.payment.dto.request.CreatePaymentRequest;
import org.project.appointment_project.payment.dto.request.PaymentCallbackRequest;
import org.project.appointment_project.payment.dto.response.PaymentResponse;
import org.project.appointment_project.payment.dto.response.PaymentUrlResponse;
import org.project.appointment_project.payment.enums.PaymentMethod;
import org.project.appointment_project.payment.enums.PaymentStatus;
import org.project.appointment_project.payment.enums.PaymentType;
import org.project.appointment_project.payment.gateway.PaymentGatewayFactory;
import org.project.appointment_project.payment.gateway.dto.PaymentGatewayRequest;
import org.project.appointment_project.payment.gateway.dto.PaymentGatewayResponse;
import org.project.appointment_project.payment.gateway.dto.PaymentVerificationResult;
import org.project.appointment_project.payment.mapper.PaymentMapper;
import org.project.appointment_project.payment.model.Payment;
import org.project.appointment_project.payment.repository.PaymentRepository;
import org.project.appointment_project.payment.service.PaymentService;
import org.project.appointment_project.payment.service.PaymentValidationService;
import org.project.appointment_project.schedule.service.SlotStatusService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentServiceImpl implements PaymentService {
    PaymentRepository paymentRepository;
    PaymentValidationService validationService;
    PaymentGatewayFactory paymentGatewayFactory;
    PaymentMapper paymentMapper;
    AppointmentRepository appointmentRepository;
    SlotStatusService slotStatusService;

    @Override
    public PaymentUrlResponse createPayment(CreatePaymentRequest request, String customerIp) {
        validationService.validateCreatePaymentRequest(request);

        // Create payment
        Payment payment = paymentMapper.toEntity(request);
        payment.setTransactionId(generateTransactionId());
        payment.setPaymentStatus(PaymentStatus.PENDING);

        Payment savedPayment = paymentRepository.save(payment);

        // Create payment URL
        var gateway = paymentGatewayFactory.getGateway(request.getPaymentMethod());

        PaymentGatewayRequest gatewayRequest = PaymentGatewayRequest.builder()
                .orderInfo("Thanh toán đặt lịch cho mã đặt: " + request.getAppointmentId())
                .customerIp(customerIp)
                .build();

        PaymentGatewayResponse gatewayResponse = gateway.createPaymentUrl(savedPayment, gatewayRequest);
        if (!gatewayResponse.isSuccess()) {
            throw new CustomException(ErrorCode.PAYMENT_GATEWAY_ERROR, gatewayResponse.getMessage());
        }

        savedPayment.setPaymentUrl(gatewayResponse.getPaymentUrl());
        savedPayment.setPaymentStatus(PaymentStatus.PROCESSING);
        paymentRepository.save(savedPayment);

        return PaymentUrlResponse.builder()
                .paymentId(savedPayment.getId())
                .paymentUrl(gatewayResponse.getPaymentUrl())
                .message("Payment URL created successfully")
                .build();
    }

    @Override
    public PaymentResponse processPaymentCallback(PaymentCallbackRequest callbackRequest) {
        // lấy transactionId gửi tới VNPAY từ callback
        String transactionId = callbackRequest.getParameters().get("vnp_TxnRef");
        if (transactionId == null) {
            throw new CustomException(ErrorCode.VNPAY_INVALID_RESPONSE);
        }

        // Tìm payment với transactionId
        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        // Kiểm tra trạng thái payment
        if (payment.getPaymentStatus() == PaymentStatus.COMPLETED) {
            throw new CustomException(ErrorCode.PAYMENT_ALREADY_PROCESSED);
        }

        if (payment.getPaymentStatus() == PaymentStatus.CANCELLED) {
            throw new CustomException(ErrorCode.PAYMENT_CANCELLED);
        }

        // Kiểm tra payment với payment gateway
        var gateway = paymentGatewayFactory.getGateway(payment.getPaymentMethod());
        PaymentVerificationResult verificationResult = gateway.verifyPayment(callbackRequest);

        if (!verificationResult.isValid()) {
            payment.setPaymentStatus(PaymentStatus.FAILED);
            payment.setGatewayResponse(verificationResult.getMessage());
            paymentRepository.save(payment);

            handlePaymentFailure(payment.getId());
            throw new CustomException(ErrorCode.VNPAY_SIGNATURE_VERIFICATION_FAILED);
        }

        // Cập nhật payment
        payment.setPaymentStatus(verificationResult.getStatus());
        payment.setGatewayTransactionId(verificationResult.getGatewayTransactionId());
        payment.setGatewayResponse(verificationResult.getResponseData());

        if (verificationResult.getStatus() == PaymentStatus.COMPLETED) {
            payment.setPaymentDate(LocalDateTime.now());
            handlePaymentSuccess(payment.getId());
        }

        Payment updatedPayment = paymentRepository.save(payment);

        log.info("Payment callback processed for ID: {}, Status: {}",
                payment.getId(), payment.getPaymentStatus());

        return paymentMapper.toResponse(updatedPayment);
    }

    @Override
    public PaymentResponse getPaymentById(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        return paymentMapper.toResponse(payment);
    }

    @Override
    public PaymentResponse cancelPayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

        validationService.validatePaymentCancellation(payment);

        payment.setPaymentStatus(PaymentStatus.CANCELLED);
        Payment updatedPayment = paymentRepository.save(payment);

        log.info("Payment cancelled: {}", paymentId);

        return paymentMapper.toResponse(updatedPayment);
    }

    @Override
    public PaymentUrlResponse createDepositPayment(UUID appointmentId, String customerIp, String returnUrl, String cancelUrl) {
        // Tìm appointment
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new CustomException(ErrorCode.APPOINTMENT_NOT_FOUND));

        // Kiểm tra appointment có thể thanh toán không
        if (appointment.getStatus() != Status.PENDING) {
            throw new CustomException(ErrorCode.APPOINTMENT_NOT_PAYABLE);
        }

        // Kiểm tra đã có payment chưa
        boolean hasExistingPayment = paymentRepository.existsByAppointmentIdAndPaymentTypeAndPaymentStatusIn(
                appointmentId, PaymentType.DEPOSIT,
                Arrays.asList(PaymentStatus.PENDING, PaymentStatus.PROCESSING, PaymentStatus.COMPLETED));

        if (hasExistingPayment) {
            throw new CustomException(ErrorCode.PAYMENT_ALREADY_EXISTS);
        }

        // Tính tiền cọc (30% consultation fee)
        BigDecimal depositAmount = appointment.getConsultationFee()
                .multiply(BigDecimal.valueOf(0.3))
                .setScale(2, RoundingMode.HALF_UP);

        Payment payment = Payment.builder()
                .appointment(appointment)
                .amount(depositAmount)
                .paymentType(PaymentType.DEPOSIT)
                .paymentMethod(PaymentMethod.VNPAY)
                .paymentStatus(PaymentStatus.PENDING)
                .transactionId(generateTransactionId())
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        var gateway = paymentGatewayFactory.getGateway(payment.getPaymentMethod());

        PaymentGatewayRequest gatewayRequest = PaymentGatewayRequest.builder()
                .orderInfo("Thanh toán một phần cho lịch: " + appointmentId)
                .customerIp(customerIp)
                .build();

        PaymentGatewayResponse gatewayResponse = gateway.createPaymentUrl(savedPayment, gatewayRequest);
        if (!gatewayResponse.isSuccess()) {
            throw new CustomException(ErrorCode.PAYMENT_GATEWAY_ERROR, gatewayResponse.getMessage());
        }

        savedPayment.setPaymentUrl(gatewayResponse.getPaymentUrl());
        savedPayment.setPaymentStatus(PaymentStatus.PROCESSING);
        paymentRepository.save(savedPayment);

        log.info("Created deposit payment {} for appointment {}", savedPayment.getId(), appointmentId);

        return PaymentUrlResponse.builder()
                .paymentId(savedPayment.getId())
                .paymentUrl(gatewayResponse.getPaymentUrl())
                .message("Deposit payment URL created successfully")
                .build();
    }

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

        // Appointment vẫn giữ trạng thái PENDING để user có thể thử thanh toán lại
        log.info("Payment {} failed, appointment {} remains PENDING",
                paymentId, payment.getAppointment().getId());
    }

    @Override
    public void processExpiredPayments() {
        LocalDateTime expiredTime = LocalDateTime.now().minusMinutes(15);

        // Tìm các appointment PENDING đã quá 15 phút
        List<Appointment> expiredAppointments = appointmentRepository
                .findExpiredPendingAppointments(expiredTime);

        log.info("Found {} expired appointments to process", expiredAppointments.size());

        for (Appointment appointment : expiredAppointments) {
            processExpiredAppointment(appointment.getId());
        }
    }

    private String generateTransactionId() {
        return "TXN" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processExpiredAppointment(UUID appointmentId) {
        try {
            // Tìm lại appointment trong transaction mới
            Appointment appointment = appointmentRepository.findById(appointmentId)
                    .orElse(null);

            if (appointment == null || appointment.getStatus() != Status.PENDING) {
                log.debug("Appointment {} no longer needs processing", appointmentId);
                return;
            }

            // Kiểm tra lại thời gian tạo để đảm bảo thực sự hết hạn
            if (appointment.getCreatedAt().isAfter(LocalDateTime.now().minusMinutes(15))) {
                log.debug("Appointment {} not yet expired", appointmentId);
                return;
            }

            // Hủy appointment
            appointment.setStatus(Status.CANCELLED);
            appointmentRepository.save(appointment);

            // Cập nhật payment nếu có
            List<Payment> pendingPayments = paymentRepository
                    .findValidPaymentsByAppointmentIdAndStatus(
                            appointmentId,
                            Arrays.asList(PaymentStatus.PENDING, PaymentStatus.PROCESSING));

            for (Payment payment : pendingPayments) {
                payment.setPaymentStatus(PaymentStatus.CANCELLED);
                payment.setNotes("Cancelled due to expired appointment");
                paymentRepository.save(payment);
            }

            // Giải phóng slot
            if (appointment.getSlot() != null) {
                slotStatusService.releaseSlot(appointment.getSlot().getId());
            }

            log.info("Successfully processed expired appointment {} with {} payments",
                    appointmentId, pendingPayments.size());

        } catch (Exception e) {
            log.error("Error processing expired appointment {}: {}", appointmentId, e.getMessage());
            throw new RuntimeException("Failed to process expired appointment: " + appointmentId);
        }
    }
}
