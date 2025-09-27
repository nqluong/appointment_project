package org.project.appointment_project.payment.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.project.appointment_project.appoinment.enums.Status;
import org.project.appointment_project.appoinment.model.Appointment;
import org.project.appointment_project.appoinment.repository.AppointmentRepository;
import org.project.appointment_project.common.exception.CustomException;
import org.project.appointment_project.common.exception.ErrorCode;
import org.project.appointment_project.notification.service.NotificationService;
import org.project.appointment_project.payment.dto.request.CreatePaymentRequest;
import org.project.appointment_project.payment.dto.request.PaymentCallbackRequest;
import org.project.appointment_project.payment.dto.request.PaymentRefundRequest;
import org.project.appointment_project.payment.dto.response.PaymentRefundResponse;
import org.project.appointment_project.payment.dto.response.PaymentResponse;
import org.project.appointment_project.payment.dto.response.PaymentUrlResponse;
import org.project.appointment_project.payment.dto.response.RefundCalculationResult;
import org.project.appointment_project.payment.enums.PaymentStatus;
import org.project.appointment_project.payment.enums.PaymentType;
import org.project.appointment_project.payment.gateway.PaymentGateway;
import org.project.appointment_project.payment.gateway.PaymentGatewayFactory;
import org.project.appointment_project.payment.gateway.dto.PaymentGatewayRequest;
import org.project.appointment_project.payment.gateway.dto.PaymentGatewayResponse;
import org.project.appointment_project.payment.gateway.dto.PaymentRefundResult;
import org.project.appointment_project.payment.gateway.dto.PaymentVerificationResult;
import org.project.appointment_project.payment.gateway.dto.RefundRequest;
import org.project.appointment_project.payment.gateway.vnpay.config.VNPayConfig;
import org.project.appointment_project.payment.mapper.PaymentMapper;
import org.project.appointment_project.payment.model.Payment;
import org.project.appointment_project.payment.repository.PaymentRepository;
import org.project.appointment_project.payment.service.AppointmentExpirationService;
import org.project.appointment_project.payment.service.OrderInfoBuilder;
import org.project.appointment_project.payment.service.PaymentAmountCalculator;
import org.project.appointment_project.payment.service.PaymentQueryService;
import org.project.appointment_project.payment.service.PaymentResolutionService;
import org.project.appointment_project.payment.service.PaymentService;
import org.project.appointment_project.payment.service.PaymentStatusHandler;
import org.project.appointment_project.payment.service.PaymentValidationService;
import org.project.appointment_project.payment.service.RefundPolicyService;
import org.project.appointment_project.payment.service.TransactionIdGenerator;
import org.project.appointment_project.payment.util.PaymentRefundUtil;
import org.project.appointment_project.schedule.service.SlotStatusService;
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
public class PaymentServiceImpl implements PaymentService {

    PaymentRepository paymentRepository;
    PaymentValidationService validationService;
    PaymentGatewayFactory paymentGatewayFactory;
    PaymentMapper paymentMapper;
    AppointmentRepository appointmentRepository;
    VNPayConfig vnPayConfig;
    SlotStatusService slotStatusService;
    NotificationService notificationService;

    PaymentAmountCalculator paymentAmountCalculator;
    PaymentStatusHandler paymentStatusHandler;
    PaymentQueryService paymentQueryService;
    AppointmentExpirationService appointmentExpirationService;
    OrderInfoBuilder orderInfoBuilder;
    PaymentRefundValidationService paymentRefundValidationService;
    PaymentRefundUtil paymentRefundUtil;
    PaymentResolutionService paymentResolutionService;
    RefundPolicyService refundPolicyService;
    TransactionIdGenerator transactionIdGenerator;

    // Tạo url thanh toán cho appointment
    @Override
    public PaymentUrlResponse createPayment(CreatePaymentRequest request, String customerIp) {
        validationService.validateCreatePaymentRequest(request);

        Appointment appointment = findAndValidateAppointment(request.getAppointmentId());

        BigDecimal paymentAmount = paymentAmountCalculator
                .calculatePaymentAmount(appointment, request.getPaymentType());

        validateNoExistingPayment(request.getAppointmentId(), request.getPaymentType());

        Payment payment = createPaymentRecord(appointment, request, paymentAmount);

        // Tạo URL thanh toán
        String paymentUrl = generatePaymentUrl(payment, request, customerIp);

        // Cập nhật trạng thái payment
        updatePaymentWithUrl(payment, paymentUrl);

        logPaymentCreation(request, payment);

        return buildPaymentUrlResponse(payment, paymentUrl);
    }

    // Xử lý callback khi thanh toán xong
    @Override
    public PaymentResponse processPaymentCallback(PaymentCallbackRequest callbackRequest) {
        String transactionId = extractTransactionId(callbackRequest);
        Payment payment = getPaymentByTransactionId(transactionId);

        validatePaymentForCallback(payment);

        PaymentVerificationResult verificationResult = verifyPaymentWithGateway(payment, callbackRequest);

        updatePaymentFromVerification(payment, verificationResult);

        Payment updatedPayment = paymentRepository.save(payment);

        if(PaymentStatus.COMPLETED.equals(updatedPayment.getPaymentStatus())) {
            notificationService.sendPaymentSuccessNotification(updatedPayment);
        }

        log.info("Đã xử lý phản hồi thanh toán cho ID: {}, Trạng thái: {}",
                payment.getId(), payment.getPaymentStatus());

        return paymentMapper.toResponse(updatedPayment);
    }

    // Xử lý hoàn tiền khi hủy
    @Override
    public PaymentRefundResponse refundPayment(PaymentRefundRequest request) {
        // Validate request
        paymentRefundValidationService.validateRefundRequest(request);

        Payment payment = paymentResolutionService.resolvePayment(
                request.getPaymentId(), request.getAppointmentId());

        paymentRefundValidationService.validatePaymentForRefund(payment, request);

        // Tính phần trăm hoàn tiền dựa trên thời gian hủy
        RefundCalculationResult calculationResult = calculateRefundAmount(payment, request);

        // Xử lý hoàn tiền qua cổng thanh toán
        String refundTxnId = transactionIdGenerator.generateRefundTransactionId();
        PaymentRefundResult gatewayResult = processRefundThroughGateway(
                payment, calculationResult.getRefundAmount(), refundTxnId, request);
        return handleRefundResult(payment, gatewayResult, refundTxnId, calculationResult);
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

        log.info("Đã hủy thanh toán: {}", paymentId);

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
    public void processProcessingPayments() {
        paymentQueryService.processProcessingPayments();
    }

     // Xác thực appointment có thể thanh toán
    private Appointment findAndValidateAppointment(UUID appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new CustomException(ErrorCode.APPOINTMENT_NOT_FOUND));

        if (appointment.getStatus() != Status.PENDING) {
            throw new CustomException(ErrorCode.APPOINTMENT_NOT_PAYABLE);
        }

        return appointment;
    }

    // Kiểm tra payment nào đã tồn tại với cùng cuộc hẹn và loại thanh toán
    private void validateNoExistingPayment(UUID appointmentId, PaymentType paymentType) {
        List<PaymentStatus> activeStatuses = Arrays.asList(
                PaymentStatus.PENDING,
                PaymentStatus.PROCESSING,
                PaymentStatus.COMPLETED
        );

        boolean hasExistingPayment = paymentRepository
                .existsByAppointmentIdAndPaymentTypeAndPaymentStatusIn(
                        appointmentId, paymentType, activeStatuses);

        if (hasExistingPayment) {
            throw new CustomException(ErrorCode.PAYMENT_ALREADY_EXISTS);
        }
    }

    // Tạo payment
    private Payment createPaymentRecord(Appointment appointment,
                                        CreatePaymentRequest request,
                                        BigDecimal paymentAmount) {
        Payment payment = Payment.builder()
                .appointment(appointment)
                .amount(paymentAmount)
                .paymentType(request.getPaymentType())
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus(PaymentStatus.PENDING)
                .transactionId(transactionIdGenerator.generateTransactionId())
                .notes(request.getNotes())
                .build();

        return paymentRepository.save(payment);
    }

    // Tạo url thanh toán
    private String generatePaymentUrl(Payment payment,
                                      CreatePaymentRequest request,
                                      String customerIp) {
        PaymentGateway gateway = paymentGatewayFactory.getGateway(request.getPaymentMethod());
        String orderInfo = orderInfoBuilder.buildOrderInfo(
                request.getPaymentType(),
                request.getAppointmentId()
        );

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

    private PaymentUrlResponse buildPaymentUrlResponse(Payment payment, String paymentUrl) {
        return PaymentUrlResponse.builder()
                .paymentId(payment.getId())
                .paymentUrl(paymentUrl)
                .message("Tạo URL thanh toán thành công")
                .build();
    }

    // Tính số tiền có thể hoàn
    private RefundCalculationResult calculateRefundAmount(Payment payment, PaymentRefundRequest request) {
        BigDecimal refundPercentage = refundPolicyService.calculateRefundPercentage(
                payment.getAppointment().getAppointmentDate(),
                LocalDateTime.now()
        );

        BigDecimal refundAmount = refundPolicyService.calculateRefundAmount(payment, request);

        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException(ErrorCode.INVALID_REFUND_AMOUNT,
                    "Số tiền hoàn lại phải lớn hơn 0");
        }

        return new RefundCalculationResult(refundAmount, refundPercentage);
    }

    // Xử lý hoàn tiền
    private PaymentRefundResponse handleRefundResult(Payment payment,
                                                     PaymentRefundResult gatewayResult,
                                                     String refundTxnId,
                                                     RefundCalculationResult calculationResult) {
        if (gatewayResult.isSuccess()) {
            updatePaymentAfterSuccessfulRefund(payment, gatewayResult, refundTxnId, calculationResult);
            handleAppointmentAndSlotAfterRefund(payment);

            log.info("Hoàn tiền thành công cho thanh toán: {}, số tiền: {}",
                    payment.getId(), calculationResult.getRefundAmount());
        } else {
            log.error("Hoàn tiền thất bại cho thanh toán: {}, lỗi: {}",
                    payment.getId(), gatewayResult.getMessage());
            throw new CustomException(ErrorCode.REFUND_PROCESSING_ERROR, gatewayResult.getMessage());
        }

        return buildRefundResponse(payment, gatewayResult, refundTxnId, calculationResult);
    }

    private void updatePaymentAfterSuccessfulRefund(Payment payment,
                                                    PaymentRefundResult gatewayResult,
                                                    String refundTxnId,
                                                    RefundCalculationResult calculationResult) {
        paymentRefundUtil.updateRefundInfo(
                payment,
                calculationResult.getRefundAmount(),
                refundTxnId,
                gatewayResult.getGatewayRefundId(),
                "Hoàn tiền theo yêu cầu khách hàng",
                gatewayResult.getRawResponse()
        );
        paymentRepository.save(payment);
    }

    private PaymentRefundResponse buildRefundResponse(Payment payment,
                                                      PaymentRefundResult gatewayResult,
                                                      String refundTxnId,
                                                      RefundCalculationResult calculationResult) {
        return PaymentRefundResponse.builder()
                .paymentId(payment.getId())
                .refundTransactionId(refundTxnId)
                .gatewayRefundId(gatewayResult.getGatewayRefundId())
                .refundAmount(calculationResult.getRefundAmount())
                .totalRefundedAmount(payment.getRefundedAmount())
                .paymentStatus(payment.getPaymentStatus())
                .message(gatewayResult.getMessage())
                .refundDate(payment.getRefundDate())
                .success(gatewayResult.isSuccess())
                .refundPercentage(calculationResult.getRefundPercentage().multiply(BigDecimal.valueOf(100)))
                .build();
    }

    private void handleAppointmentAndSlotAfterRefund(Payment payment) {
        try {
            Appointment appointment = payment.getAppointment();

            // Cập nhật trạng thái appointment thành CANCELLED
            if (appointment.getStatus() != Status.CANCELLED) {
                appointment.setStatus(Status.CANCELLED);
                appointmentRepository.save(appointment);
                log.info("Đã cập nhật trạng thái lịch hẹn thành ĐÃ HỦY cho lịch hẹn có ID: {}",
                        appointment.getId());
            }

            // Giải phóng slot để người khác có thể đặt
            if (appointment.getSlot() != null) {
                try {
                    slotStatusService.releaseSlot(appointment.getSlot().getId());
                    log.info("Đã giải phóng thành công khung giờ ID: {} sau khi hoàn tiền cho lịch hẹn ID: {}",
                            appointment.getSlot().getId(), appointment.getId());
                } catch (Exception e) {
                    log.error("Không thể giải phóng khung giờ ID: {} sau khi hoàn tiền cho lịch hẹn ID: {}. Lỗi: {}",
                            appointment.getSlot().getId(), appointment.getId(), e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Lỗi xử lý lịch hẹn và khung giờ sau khi hoàn tiền cho thanh toán ID: {}. Lỗi: {}",
                    payment.getId(), e.getMessage());
        }
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
                        .orderInfo("Hoan tien cho giao dich: " + payment.getTransactionId())
                        .build();

        return gateway.refundPayment(gatewayRequest);
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
                ? "Đã tạo thanh toán đặt cọc {} cho lịch hẹn {}"
                : "Đã tạo thanh toán toàn bộ {} cho lịch hẹn {}";
        log.info(logMessage, savedPayment.getId(), request.getAppointmentId());
    }
}
