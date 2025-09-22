package org.project.appointment_project.payment.gateway.vnpay;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.common.exception.CustomException;
import org.project.appointment_project.common.exception.ErrorCode;
import org.project.appointment_project.payment.dto.request.PaymentCallbackRequest;
import org.project.appointment_project.payment.enums.PaymentMethod;
import org.project.appointment_project.payment.enums.PaymentStatus;
import org.project.appointment_project.payment.gateway.PaymentGateway;
import org.project.appointment_project.payment.gateway.dto.PaymentGatewayRequest;
import org.project.appointment_project.payment.gateway.dto.PaymentGatewayResponse;
import org.project.appointment_project.payment.gateway.dto.PaymentQueryResult;
import org.project.appointment_project.payment.gateway.dto.PaymentVerificationResult;
import org.project.appointment_project.payment.gateway.vnpay.config.VNPayConfig;
import org.project.appointment_project.payment.gateway.vnpay.util.VNPayUtil;
import org.project.appointment_project.payment.model.Payment;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VNPayGateway implements PaymentGateway {
    VNPayConfig vnPayConfig;
    VNPayUtil vnPayUtil;
    RestTemplate restTemplate;
    ObjectMapper objectMapper;

    @Override
    public PaymentGatewayResponse createPaymentUrl(Payment payment, PaymentGatewayRequest request) {
        try {
            Map<String, String> params = buildPaymentParams(payment, request);
            String paymentUrl = vnPayUtil.buildPaymentUrl(vnPayConfig.getPaymentUrl(), params, vnPayConfig.getHashSecret());

            return PaymentGatewayResponse.builder()
                    .success(true)
                    .paymentUrl(paymentUrl)
                    .transactionId(payment.getTransactionId())
                    .message("Payment URL created successfully")
                    .build();

        } catch (Exception e) {
            log.error("Error creating VNPay payment URL for transaction: {}", payment.getTransactionId(), e);
            throw new CustomException(ErrorCode.PAYMENT_GATEWAY_ERROR, e.getMessage());
        }
    }

    @Override
    public PaymentVerificationResult verifyPayment(PaymentCallbackRequest callbackRequest) {
        try {
            Map<String, String> params = callbackRequest.getParameters();

            if (!vnPayUtil.verifyCallback(params, vnPayConfig.getHashSecret())) {
                log.error("VNPay callback signature verification failed");
                return PaymentVerificationResult.builder()
                        .valid(false)
                        .message("Invalid signature")
                        .build();
            }

            return buildVerificationResult(params);
        } catch (Exception e) {
            log.error("Error verifying VNPay payment", e);
            throw new CustomException(ErrorCode.VNPAY_SIGNATURE_VERIFICATION_FAILED, e);
        }
    }

    @Override
    public PaymentQueryResult queryPaymentStatus(String transactionId, String transactionDate) {
        try {
            log.info("Querying VNPay payment status for transaction: {}", transactionId);

            Map<String, Object> requestBody = buildQueryRequest(transactionId, transactionDate);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    vnPayConfig.getQueryUrl(),
                    entity,
                    String.class
            );

            return parseQueryResponse(response.getBody(), transactionId);

        } catch (Exception e) {
            log.error("Error querying VNPay payment status for transaction: {}", transactionId, e);
            return PaymentQueryResult.builder()
                    .success(false)
                    .status(PaymentStatus.FAILED)
                    .message(e.getMessage())
                    .build();
        }
    }

    @Override
    public boolean supports(PaymentMethod paymentMethod) {
        return PaymentMethod.VNPAY.equals(paymentMethod);
    }

    private Map<String, String> buildPaymentParams(Payment payment, PaymentGatewayRequest request) {
        long amount = payment.getAmount().multiply(BigDecimal.valueOf(100)).longValue();
        String createDate = vnPayUtil.getCurrentDateTime();

        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", vnPayConfig.getVersion());
        params.put("vnp_Command", vnPayConfig.getCommand());
        params.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        params.put("vnp_Amount", String.valueOf(amount));
        params.put("vnp_CurrCode", vnPayConfig.getCurrCode());
        params.put("vnp_TxnRef", payment.getTransactionId());
        params.put("vnp_OrderInfo", request.getOrderInfo());
        params.put("vnp_OrderType", vnPayConfig.getOrderType());
        params.put("vnp_Locale", vnPayConfig.getLocale());
        params.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        params.put("vnp_IpAddr", request.getCustomerIp());
        params.put("vnp_CreateDate", createDate);

        // Set expire date (15 minutes from now)
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        calendar.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(calendar.getTime());
        params.put("vnp_ExpireDate", vnp_ExpireDate);

        return params;
    }

    private PaymentVerificationResult buildVerificationResult(Map<String, String> params) {
        String responseCode = params.get("vnp_ResponseCode");
        String txnRef = params.get("vnp_TxnRef");
        String transactionNo = params.get("vnp_TransactionNo");
        String amountStr = params.get("vnp_Amount");

        PaymentStatus status = "00".equals(responseCode) ?
                PaymentStatus.COMPLETED : PaymentStatus.FAILED;

        BigDecimal amount = amountStr != null ?
                new BigDecimal(amountStr).divide(BigDecimal.valueOf(100)) : null;

        return PaymentVerificationResult.builder()
                .valid(true)
                .transactionId(txnRef)
                .gatewayTransactionId(transactionNo)
                .amount(amount)
                .status(status)
                .message(getResponseMessage(responseCode))
                .responseData(params.toString())
                .build();
    }

    private Map<String, Object> buildQueryRequest(String transactionId, String transactionDate) {
        String requestId = vnPayUtil.generateRequestId();
        String createDate = vnPayUtil.getCurrentDateTime();
        String orderInfo = "Query transaction: " + transactionId;

        // Use provided transactionDate or extract from transactionId
        String txnDate = (transactionDate != null) ? transactionDate :
                vnPayUtil.extractTransactionDate(transactionId);

        String secureHash = vnPayUtil.createQuerySecureHash(
                requestId,
                vnPayConfig.getVersion(),
                "querydr",
                vnPayConfig.getTmnCode(),
                transactionId,
                txnDate,
                createDate,
                "127.0.0.1",
                orderInfo,
                vnPayConfig.getHashSecret()
        );

        Map<String, Object> request = new HashMap<>();
        request.put("vnp_RequestId", requestId);
        request.put("vnp_Version", vnPayConfig.getVersion());
        request.put("vnp_Command", "querydr");
        request.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        request.put("vnp_TxnRef", transactionId);
        request.put("vnp_OrderInfo", orderInfo);
        request.put("vnp_TransactionDate", txnDate);
        request.put("vnp_CreateDate", createDate);
        request.put("vnp_IpAddr", "127.0.0.1");
        request.put("vnp_SecureHash", secureHash);

        return request;
    }

    private PaymentQueryResult parseQueryResponse(String responseBody, String transactionId) {
        try {

            Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);

            String responseCode = (String) response.get("vnp_ResponseCode");
            String transactionStatus = (String) response.get("vnp_TransactionStatus");
            String vnpTransactionNo = (String) response.get("vnp_TransactionNo");
            String amountStr = (String) response.get("vnp_Amount");
            String payDate = (String) response.get("vnp_PayDate");

            if (!"00".equals(responseCode)) {
                return PaymentQueryResult.builder()
                        .success(false)
                        .status(PaymentStatus.FAILED)
                        .transactionId(transactionId)
                        .message("Query failed with response code: " + responseCode)
                        .rawResponse(responseBody)
                        .build();
            }

            PaymentStatus status = determinePaymentStatus(transactionStatus);
            BigDecimal amount = amountStr != null ?
                    new BigDecimal(amountStr).divide(BigDecimal.valueOf(100)) : null;

            LocalDateTime paymentDate = parsePaymentDate(payDate);

            return PaymentQueryResult.builder()
                    .success(true)
                    .status(status)
                    .transactionId(transactionId)
                    .gatewayTransactionId(vnpTransactionNo)
                    .amount(amount)
                    .responseCode(transactionStatus)
                    .paymentDate(paymentDate)
                    .message(getTransactionStatusMessage(transactionStatus))
                    .rawResponse(responseBody)
                    .build();

        } catch (Exception e) {
            log.error("Error parsing VNPay query response", e);
            return PaymentQueryResult.builder()
                    .success(false)
                    .status(PaymentStatus.FAILED)
                    .message("Error parsing VNPay query response: " + e.getMessage())
                    .build();

        }
    }


    private PaymentStatus determinePaymentStatus(String transactionStatus) {
        if (transactionStatus == null) {
            return PaymentStatus.FAILED;
        }

        switch (transactionStatus) {
            case "00": return PaymentStatus.COMPLETED;
            case "01": return PaymentStatus.PENDING;
            default: return PaymentStatus.FAILED;
        }
    }

    private LocalDateTime parsePaymentDate(String payDate) {
        if (payDate == null || payDate.isEmpty()) {
            return null;
        }

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            return LocalDateTime.parse(payDate, formatter);
        } catch (Exception e) {
            log.warn("Error parsing payment date: {}", payDate, e);
            return null;
        }
    }

    private String extractTransactionDate(String transactionId) {
        // Extract timestamp from transaction ID (assuming format: TXN{timestamp}{uuid})
        try {
            String timestampStr = transactionId.substring(3, 16); // Remove "TXN" and get timestamp
            long timestamp = Long.parseLong(timestampStr);
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
            cal.setTimeInMillis(timestamp);

            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
            return formatter.format(cal.getTime());
        } catch (Exception e) {
            // Fallback to current date
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
            return formatter.format(cal.getTime());
        }
    }

    private String getTransactionStatusMessage(String status) {
        switch (status) {
            case "00": return "Giao dịch thanh toán được thực hiện thành công";
            case "01": return "Giao dịch chưa hoàn tất";
            case "02": return "Giao dịch bị lỗi";
            case "04": return "Giao dịch đảo (Khách hàng đã bị trừ tiền tại Ngân hàng nhưng GD chưa thành công ở VNPAY)";
            case "05": return "VNPAY đang xử lý giao dịch này (GD hoàn tiền)";
            case "06": return "VNPAY đã gửi yêu cầu hoàn tiền sang Ngân hàng (GD hoàn tiền)";
            case "07": return "Giao dịch bị nghi ngờ";
            case "09": return "GD Hoàn trả bị từ chối";
            default: return "Trạng thái không xác định";
        }
    }

    private String getResponseMessage(String responseCode) {
        switch (responseCode) {
            case "00": return "Transaction successful";
            case "07": return "Trừ tiền thành công. Giao dịch bị nghi ngờ";
            case "09": return "Giao dịch không thành công do: Thẻ/Tài khoản của khách hàng chưa đăng ký dịch vụ InternetBanking tại ngân hàng";
            case "10": return "Giao dịch không thành công do: Khách hàng xác thực thông tin thẻ/tài khoản không đúng quá 3 lần";
            case "11": return "Giao dịch không thành công do: Đã hết hạn chờ thanh toán";
            case "12": return "Giao dịch không thành công do: Thẻ/Tài khoản của khách hàng bị khóa";
            case "13": return "Giao dịch không thành công do Quý khách nhập sai mật khẩu xác thực giao dịch";
            case "24": return "Giao dịch không thành công do: Khách hàng hủy giao dịch";
            case "51": return "Giao dịch không thành công do: Tài khoản của quý khách không đủ số dư để thực hiện giao dịch";
            case "65": return "Giao dịch không thành công do: Tài khoản của Quý khách đã vượt quá hạn mức giao dịch trong ngày";
            case "75": return "Ngân hàng thanh toán đang bảo trì";
            case "79": return "Giao dịch không thành công do: KH nhập sai mật khẩu thanh toán quá số lần quy định";
            default: return "Giao dịch thất bại";
        }
    }
}
