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
import org.project.appointment_project.payment.dto.request.PaymentRefundRequest;
import org.project.appointment_project.payment.dto.response.PaymentRefundResponse;
import org.project.appointment_project.payment.dto.response.PaymentResponse;
import org.project.appointment_project.payment.dto.response.PaymentUrlResponse;
import org.project.appointment_project.payment.enums.PaymentStatus;
import org.project.appointment_project.payment.enums.PaymentType;
import org.project.appointment_project.payment.gateway.PaymentGateway;
import org.project.appointment_project.payment.gateway.PaymentGatewayFactory;
import org.project.appointment_project.payment.gateway.dto.*;
import org.project.appointment_project.payment.gateway.vnpay.config.VNPayConfig;
import org.project.appointment_project.payment.mapper.PaymentMapper;
import org.project.appointment_project.payment.model.Payment;
import org.project.appointment_project.payment.repository.PaymentRepository;
import org.project.appointment_project.payment.service.*;
import org.project.appointment_project.payment.util.PaymentRefundUtil;
import org.project.appointment_project.schedule.service.SlotStatusService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
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
    SlotStatusService slotStatusService;

    PaymentAmountCalculator paymentAmountCalculator;
    PaymentStatusHandler paymentStatusHandler;
    PaymentQueryService paymentQueryService;
    AppointmentExpirationService appointmentExpirationService;
    OrderInfoBuilder orderInfoBuilder;
    PaymentRefundValidationService paymentRefundValidationService;
    PaymentRefundUtil paymentRefundUtil;
    PaymentResolutionService paymentResolutionService;
    RefundPolicyService refundPolicyService;

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
    public PaymentRefundResponse refundPayment(PaymentRefundRequest request) {
        // Validate request
        paymentRefundValidationService.validateRefundRequest(request);

        Payment payment = paymentResolutionService.resolvePayment(
                request.getPaymentId(), request.getAppointmentId());

        paymentRefundValidationService.validatePaymentForRefund(payment, request);

        // Tính phần trăm hoàn tiền dựa trên thời gian hủy
        BigDecimal refundPercentage = refundPolicyService.calculateRefundPercentage(
                payment.getAppointment().getAppointmentDate(),
                LocalDateTime.now());

        BigDecimal refundAmount = refundPolicyService.calculateRefundAmount(payment, refundPercentage);

        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException(ErrorCode.INVALID_REFUND_AMOUNT,
                    "Calculated refund amount must be greater than 0");
        }
        String refundTxnId = generateRefundTransactionId();
        PaymentRefundResult gatewayResult = processRefundThroughGateway(
                payment, refundAmount, refundTxnId, request);

        if (gatewayResult.isSuccess()) {
            paymentRefundUtil.updateRefundInfo(
                    payment,
                    refundAmount,
                    refundTxnId,
                    gatewayResult.getGatewayRefundId(),
                    request.getReason(),
                    gatewayResult.getRawResponse()
            );
            paymentRepository.save(payment);
            handleAppointmentAndSlotAfterRefund(payment);
            log.info("Refund completed for payment: {}, amount: {}",
                    request.getPaymentId(), refundAmount);
        } else {
            log.error("Refund failed for payment: {}, error: {}",
                    request.getPaymentId(), gatewayResult.getMessage());
            throw new CustomException(ErrorCode.REFUND_PROCESSING_ERROR, gatewayResult.getMessage());
        }

        return PaymentRefundResponse.builder()
                .paymentId(payment.getId())
                .refundTransactionId(refundTxnId)
                .gatewayRefundId(gatewayResult.getGatewayRefundId())
                .refundAmount(refundAmount)
                .totalRefundedAmount(payment.getRefundedAmount())
                .paymentStatus(payment.getPaymentStatus())
                .message(gatewayResult.getMessage())
                .refundDate(payment.getRefundDate())
                .success(gatewayResult.isSuccess())
                .refundPercentage(refundPercentage.multiply(BigDecimal.valueOf(100)))
                .build();
    }

    private void handleAppointmentAndSlotAfterRefund(Payment payment) {
        try {
            Appointment appointment = payment.getAppointment();

            // Cập nhật trạng thái appointment thành CANCELLED
            if (appointment.getStatus() != Status.CANCELLED) {
                appointment.setStatus(Status.CANCELLED);
                appointmentRepository.save(appointment);
                log.info("Updated appointment status to CANCELLED for appointment ID: {}",
                        appointment.getId());
            }

            // Giải phóng slot để người khác có thể đặt
            if (appointment.getSlot() != null) {
                try {
                    slotStatusService.releaseSlot(appointment.getSlot().getId());
                    log.info("Successfully released slot ID: {} after refund for appointment ID: {}",
                            appointment.getSlot().getId(), appointment.getId());
                } catch (Exception e) {
                    log.error("Failed to release slot ID: {} after refund for appointment ID: {}. Error: {}",
                            appointment.getSlot().getId(), appointment.getId(), e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Error handling appointment and slot after refund for payment ID: {}. Error: {}",
                    payment.getId(), e.getMessage());
        }
    }

    private String generateRefundTransactionId() {
        return "RFD" + System.currentTimeMillis() +
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private PaymentRefundResult processRefundThroughGateway(Payment payment,
                                                            BigDecimal refundAmount, String refundTxnId, PaymentRefundRequest request) {

        PaymentGateway gateway = paymentGatewayFactory.getGateway(payment.getPaymentMethod());

        RefundRequest gatewayRequest =
                RefundRequest.builder()
                        .originalTransactionId(payment.getTransactionId())
                        .refundTransactionId(refundTxnId)
                        .gatewayTransactionId(payment.getGatewayTransactionId())
                        .refundAmount(refundAmount)
                        .originalAmount(payment.getAmount())
                        .reason(request.getReason())
                        .transactionDate(payment.getPaymentDate().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")))
                        .customerIp("127.0.0.1")
                        .orderInfo("Hoàn tiền cho giao dịch: " + payment.getTransactionId())
                        .build();

        return gateway.refundPayment(gatewayRequest);
    }

    @Override
    @Transactional
    public void processProcessingPayments() {
        paymentQueryService.processProcessingPayments();
    }

    private void validateExistingPayment(UUID appointmentId, PaymentType paymentType) {
        boolean hasExistingPayment = paymentRepository.existsByAppointmentIdAndPaymentTypeAndPaymentStatusIn(
                appointmentId, paymentType,
                Arrays.asList(PaymentStatus.PENDING, PaymentStatus.PROCESSING, PaymentStatus.COMPLETED));

        if (hasExistingPayment) {
            throw new CustomException(ErrorCode.PAYMENT_ALREADY_EXISTS);
        }
    }


    private String generateTransactionId() {
        return "TXN" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
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
