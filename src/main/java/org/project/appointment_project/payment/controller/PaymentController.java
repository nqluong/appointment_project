package org.project.appointment_project.payment.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.payment.dto.request.CreateDepositPaymentRequest;
import org.project.appointment_project.payment.dto.request.CreatePaymentRequest;
import org.project.appointment_project.payment.dto.request.PaymentCallbackRequest;
import org.project.appointment_project.payment.dto.response.PaymentResponse;
import org.project.appointment_project.payment.dto.response.PaymentUrlResponse;
import org.project.appointment_project.payment.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping("/deposit/{appointmentId}")
    public ResponseEntity<PaymentUrlResponse> createDepositPayment(
            @PathVariable UUID appointmentId,
            @RequestBody CreateDepositPaymentRequest request,
            HttpServletRequest httpRequest) {

        String customerIp = getClientIp(httpRequest);

        PaymentUrlResponse response = paymentService.createDepositPayment(
                appointmentId, customerIp, request.getReturnUrl(), request.getCancelUrl());

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<PaymentUrlResponse> createPayment(
            @Valid @RequestBody CreatePaymentRequest request,
            HttpServletRequest httpRequest) {

        String customerIp = getClientIp(httpRequest);

        PaymentUrlResponse response = paymentService.createPayment(request, customerIp);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(
            @PathVariable UUID paymentId) {

        PaymentResponse response = paymentService.getPaymentById(paymentId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/vnpay/callback")
    public ResponseEntity<PaymentResponse> vnpayCallback(
            @RequestParam Map<String, String> params,
            HttpServletRequest request) {
        log.info("Raw callback params: {}", params);
        PaymentCallbackRequest callbackRequest = PaymentCallbackRequest.builder()
                .parameters(params)
                .build();

        PaymentResponse response = paymentService.processPaymentCallback(callbackRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/vnpay/return")
    public ResponseEntity<Map<String, Object>> vnpayReturn(
            @RequestParam Map<String, String> params) {

        log.info("Received VNPay return with transaction: {}", params.get("vnp_TxnRef"));

        PaymentCallbackRequest callbackRequest = PaymentCallbackRequest.builder()
                .parameters(params)
                .build();

        PaymentResponse response = paymentService.processPaymentCallback(callbackRequest);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Payment processed successfully");
        result.put("payment", response);

        return ResponseEntity.ok(result);
    }

    @PutMapping("/{paymentId}/cancel")
    public ResponseEntity<PaymentResponse> cancelPayment(
           @PathVariable UUID paymentId) {

        log.info("Cancelling payment: {}", paymentId);
        PaymentResponse response = paymentService.cancelPayment(paymentId);
        return ResponseEntity.ok(response);
    }

    private String getClientIp(HttpServletRequest request) {
        String clientIp = null;

        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            clientIp = xForwardedFor.split(",")[0].trim();
        }

        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getHeader("X-Real-IP");
        }

        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getHeader("X-Forwarded");
        }

        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getHeader("Forwarded-For");
        }

        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getHeader("Forwarded");
        }

        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getRemoteAddr();
        }

        if ("0:0:0:0:0:0:0:1".equals(clientIp) || "::1".equals(clientIp)) {
            clientIp = "127.0.0.1";
        }

        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = "127.0.0.1";
        }

        log.debug("Resolved client IP: {}", clientIp);
        return clientIp;
    }

}
