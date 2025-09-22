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
import org.project.appointment_project.payment.config.PaymentQueryConfig;
import org.project.appointment_project.payment.dto.request.CreatePaymentRequest;
import org.project.appointment_project.payment.dto.request.PaymentCallbackRequest;
import org.project.appointment_project.payment.dto.response.PaymentResponse;
import org.project.appointment_project.payment.dto.response.PaymentUrlResponse;
import org.project.appointment_project.payment.enums.PaymentStatus;
import org.project.appointment_project.payment.enums.PaymentType;
import org.project.appointment_project.payment.gateway.PaymentGateway;
import org.project.appointment_project.payment.gateway.PaymentGatewayFactory;
import org.project.appointment_project.payment.gateway.dto.PaymentGatewayRequest;
import org.project.appointment_project.payment.gateway.dto.PaymentGatewayResponse;
import org.project.appointment_project.payment.gateway.dto.PaymentQueryResult;
import org.project.appointment_project.payment.gateway.dto.PaymentVerificationResult;
import org.project.appointment_project.payment.gateway.vnpay.config.VNPayConfig;
import org.project.appointment_project.payment.mapper.PaymentMapper;
import org.project.appointment_project.payment.model.Payment;
import org.project.appointment_project.payment.repository.PaymentRepository;
import org.project.appointment_project.payment.service.*;
import org.project.appointment_project.schedule.service.SlotStatusService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    VNPayConfig vnPayConfig;

    PaymentAmountCalculator paymentAmountCalculator;
    PaymentStatusHandler paymentStatusHandler;
    PaymentQueryService paymentQueryService;
    AppointmentExpirationService appointmentExpirationService;
    TransactionIdGenerator transactionIdGenerator;
    OrderInfoBuilder orderInfoBuilder;

    @Override
    public PaymentUrlResponse createPayment(CreatePaymentRequest request, String customerIp) {
        validationService.validateCreatePaymentRequest(request);

        Appointment appointment = appointmentRepository.findById(request.getAppointmentId())
                .orElseThrow(() -> new CustomException(ErrorCode.APPOINTMENT_NOT_FOUND));

        if (appointment.getStatus() != Status.PENDING) {
            throw new CustomException(ErrorCode.APPOINTMENT_NOT_PAYABLE);
        }

        BigDecimal paymentAmount = paymentAmountCalculator
                .calculatePaymentAmount(appointment, request.getPaymentType());

        // Kiểm tra payment đã tồn tại chưa
        validateExistingPayment(request.getAppointmentId(), request.getPaymentType());

        // Create payment
        Payment payment = Payment.builder()
                .appointment(appointment)
                .amount(paymentAmount)
                .paymentType(request.getPaymentType())
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus(PaymentStatus.PENDING)
                .transactionId(generateTransactionId())
                .notes(request.getNotes())
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        String paymentUrl = createPaymentUrl(savedPayment, request, customerIp);


        updatePaymentWithUrl(savedPayment, paymentUrl);

        logPaymentCreation(request, savedPayment);

        return PaymentUrlResponse.builder()
                .paymentId(savedPayment.getId())
                .paymentUrl(paymentUrl)
                .message("Payment URL created successfully")
                .build();
    }

    @Override
    public PaymentResponse processPaymentCallback(PaymentCallbackRequest callbackRequest) {
        String transactionId = extractTransactionId(callbackRequest);
        Payment payment = getPaymentByTransactionId(transactionId);

        validatePaymentForCallback(payment);

        PaymentVerificationResult verificationResult = verifyPaymentWithGateway(payment, callbackRequest);

        updatePaymentFromVerification(payment, verificationResult);

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
    public void handlePaymentSuccess(UUID paymentId) {
        paymentStatusHandler.handlePaymentSuccess(paymentId);
    }

    @Override
    public void handlePaymentFailure(UUID paymentId) {
        paymentStatusHandler.handlePaymentFailure(paymentId);
    }

    @Override
    public void processExpiredPayments() {
        appointmentExpirationService.processExpiredAppointments();
    }

    @Override
    public PaymentResponse queryPaymentStatus(UUID paymentId) {
        return paymentQueryService.queryPaymentStatus(paymentId);
    }

    @Override
    public PaymentResponse queryPaymentStatus(String transactionId) {
        return paymentQueryService.queryPaymentStatus(transactionId);
    }

    @Override
    @Transactional
    public void processProcessingPayments() {
        paymentQueryService.processProcessingPayments();
    }

    private Appointment getAppointment(UUID appointmentId) {
        return appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new CustomException(ErrorCode.APPOINTMENT_NOT_FOUND));
    }

    private void validateAppointmentForPayment(Appointment appointment) {
        if (appointment.getStatus() != Status.PENDING) {
            throw new CustomException(ErrorCode.APPOINTMENT_NOT_PAYABLE);
        }
    }

    private void validateExistingPayment(UUID appointmentId, PaymentType paymentType) {
        boolean hasExistingPayment = paymentRepository.existsByAppointmentIdAndPaymentTypeAndPaymentStatusIn(
                appointmentId, paymentType,
                Arrays.asList(PaymentStatus.PENDING, PaymentStatus.PROCESSING, PaymentStatus.COMPLETED));

        if (hasExistingPayment) {
            throw new CustomException(ErrorCode.PAYMENT_ALREADY_EXISTS);
        }
    }

    private String buildOrderInfo(PaymentType paymentType, UUID appointmentId) {
        switch (paymentType) {
            case DEPOSIT:
                return "Thanh toán tiền cọc cho lịch hẹn: " + appointmentId;
            case FULL:
                return "Thanh toán đầy đủ cho lịch hẹn: " + appointmentId;
            case REMAINING:
                return "Thanh toán số tiền còn lại cho lịch hẹn: " + appointmentId;
            default:
                return "Thanh toán cho lịch hẹn: " + appointmentId;
        }
    }

    private String generateTransactionId() {
        return "TXN" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private Payment createPaymentEntity(CreatePaymentRequest request, Appointment appointment, BigDecimal amount) {
        return Payment.builder()
                .appointment(appointment)
                .amount(amount)
                .paymentType(request.getPaymentType())
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus(PaymentStatus.PENDING)
                .transactionId(transactionIdGenerator.generateTransactionId())
                .notes(request.getNotes())
                .build();
    }

    private String createPaymentUrl(Payment payment, CreatePaymentRequest request, String customerIp) {
        PaymentGateway gateway = paymentGatewayFactory.getGateway(request.getPaymentMethod());
        String orderInfo = orderInfoBuilder.buildOrderInfo(request.getPaymentType(), request.getAppointmentId());

        PaymentGatewayRequest gatewayRequest = PaymentGatewayRequest.builder()
                .orderInfo(orderInfo)
                .customerIp(customerIp)
                .returnUrl(vnPayConfig.getReturnUrl())
                .build();

        PaymentGatewayResponse gatewayResponse = gateway.createPaymentUrl(payment, gatewayRequest);
        if (!gatewayResponse.isSuccess()) {
            throw new CustomException(ErrorCode.PAYMENT_GATEWAY_ERROR, gatewayResponse.getMessage());
        }

        return gatewayResponse.getPaymentUrl();
    }

    private void updatePaymentWithUrl(Payment payment, String paymentUrl) {
        payment.setPaymentUrl(paymentUrl);
        payment.setPaymentStatus(PaymentStatus.PROCESSING);
        paymentRepository.save(payment);
    }

    private String extractTransactionId(PaymentCallbackRequest callbackRequest) {
        String transactionId = callbackRequest.getParameters().get("vnp_TxnRef");
        if (transactionId == null) {
            throw new CustomException(ErrorCode.VNPAY_INVALID_RESPONSE);
        }
        return transactionId;
    }

    private Payment getPaymentByTransactionId(String transactionId) {
        return paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));
    }

    private void validatePaymentForCallback(Payment payment) {
        if (payment.getPaymentStatus() == PaymentStatus.COMPLETED) {
            throw new CustomException(ErrorCode.PAYMENT_ALREADY_PROCESSED);
        }
        if (payment.getPaymentStatus() == PaymentStatus.CANCELLED) {
            throw new CustomException(ErrorCode.PAYMENT_CANCELLED);
        }
    }

    private PaymentVerificationResult verifyPaymentWithGateway(Payment payment, PaymentCallbackRequest callbackRequest) {
        var gateway = paymentGatewayFactory.getGateway(payment.getPaymentMethod());
        return gateway.verifyPayment(callbackRequest);
    }

    private void updatePaymentFromVerification(Payment payment, PaymentVerificationResult verificationResult) {
        if (!verificationResult.isValid()) {
            payment.setPaymentStatus(PaymentStatus.FAILED);
            payment.setGatewayResponse(verificationResult.getMessage());
            paymentRepository.save(payment);
            handlePaymentFailure(payment.getId());
            throw new CustomException(ErrorCode.VNPAY_SIGNATURE_VERIFICATION_FAILED);
        }

        payment.setPaymentStatus(verificationResult.getStatus());
        payment.setGatewayTransactionId(verificationResult.getGatewayTransactionId());
        payment.setGatewayResponse(verificationResult.getResponseData());

        if (verificationResult.getStatus() == PaymentStatus.COMPLETED) {
            payment.setPaymentDate(LocalDateTime.now());
            handlePaymentSuccess(payment.getId());
        }
    }

    private void logPaymentCreation(CreatePaymentRequest request, Payment savedPayment) {
        String logMessage = request.getPaymentType() == PaymentType.DEPOSIT
                ? "Created deposit payment {} for appointment {}"
                : "Created full payment {} for appointment {}";
        log.info(logMessage, savedPayment.getId(), request.getAppointmentId());
    }
}
