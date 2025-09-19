package org.project.appointment_project.payment.gateway.vnpay;

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
import org.project.appointment_project.payment.gateway.dto.PaymentVerificationResult;
import org.project.appointment_project.payment.gateway.vnpay.config.VNPayConfig;
import org.project.appointment_project.payment.gateway.vnpay.util.VNPayUtil;
import org.project.appointment_project.payment.model.Payment;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VNPayGateway implements PaymentGateway {
    VNPayConfig vnPayConfig;
    VNPayUtil vnPayUtil;

    @Override
    public PaymentGatewayResponse createPaymentUrl(Payment payment, PaymentGatewayRequest request) {
        try {
            String txnRef = payment.getTransactionId();
            String orderInfo = request.getOrderInfo();
            long amount = payment.getAmount().multiply(BigDecimal.valueOf(100)).longValue();

            Map<String, String> vnp_Params = new HashMap<>();
            vnp_Params.put("vnp_Version", vnPayConfig.getVersion());
            vnp_Params.put("vnp_Command", vnPayConfig.getCommand());
            vnp_Params.put("vnp_TmnCode", vnPayConfig.getTmnCode());
            vnp_Params.put("vnp_Amount", String.valueOf(amount));
            vnp_Params.put("vnp_CurrCode", vnPayConfig.getCurrCode());
            vnp_Params.put("vnp_TxnRef", txnRef);
            vnp_Params.put("vnp_OrderInfo", orderInfo);
            vnp_Params.put("vnp_OrderType", vnPayConfig.getOrderType());
            vnp_Params.put("vnp_Locale", vnPayConfig.getLocale());
            vnp_Params.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
            vnp_Params.put("vnp_IpAddr", request.getCustomerIp());

            Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            String vnp_CreateDate = formatter.format(cld.getTime());
            vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

            cld.add(Calendar.MINUTE, 15);
            String vnp_ExpireDate = formatter.format(cld.getTime());
            vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

            List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
            Collections.sort(fieldNames);
            StringBuilder hashData = new StringBuilder();
            StringBuilder query = new StringBuilder();

            Iterator<String> itr = fieldNames.iterator();
            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = vnp_Params.get(fieldName);
                if ((fieldValue != null) && (fieldValue.length() > 0)) {
                    hashData.append(fieldName);
                    hashData.append('=');
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                    query.append('=');
                    query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    if (itr.hasNext()) {
                        query.append('&');
                        hashData.append('&');
                    }
                }
            }

            String queryUrl = query.toString();
            String vnp_SecureHash = vnPayUtil.hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());
            queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
            String paymentUrl = vnPayConfig.getPaymentUrl() + "?" + queryUrl;

            return PaymentGatewayResponse.builder()
                    .success(true)
                    .paymentUrl(paymentUrl)
                    .transactionId(txnRef)
                    .message("Payment URL created successfully")
                    .build();

        } catch (Exception e) {
            log.error("Error creating VNPay payment URL", e);
            throw new CustomException(ErrorCode.PAYMENT_GATEWAY_ERROR, e);
        }
    }

    @Override
    public PaymentVerificationResult verifyPayment(PaymentCallbackRequest callbackRequest) {
        try {
            Map<String, String> fields = callbackRequest.getParameters();

            // Lấy vnp_SecureHash trước khi xóa khỏi map
            String vnp_SecureHash = fields.get("vnp_SecureHash");
            if (vnp_SecureHash == null || vnp_SecureHash.isEmpty()) {
                return PaymentVerificationResult.builder()
                        .valid(false)
                        .message("Missing vnp_SecureHash")
                        .build();
            }

            // Tạo copy của fields để verify
            Map<String, String> fieldsToVerify = new HashMap<>(fields);
            fieldsToVerify.remove("vnp_SecureHashType");
            fieldsToVerify.remove("vnp_SecureHash");

            Map<String, String> encodedFields = new HashMap<>();
            for (Map.Entry<String, String> entry : fieldsToVerify.entrySet()) {
                try {
                    String encodedValue = URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII.toString());
                    encodedFields.put(entry.getKey(), encodedValue);
                } catch (Exception e) {
                    encodedFields.put(entry.getKey(), entry.getValue());
                }
            }
            String signValue = vnPayUtil.hashAllFields(encodedFields, vnPayConfig.getHashSecret());
            log.info("Received signature: {}", vnp_SecureHash);


            if (!signValue.equals(vnp_SecureHash)) {
                log.error("Signature verification failed");
                return PaymentVerificationResult.builder()
                        .valid(false)
                        .message("Invalid signature")
                        .build();
            }

            String vnp_ResponseCode = fields.get("vnp_ResponseCode");
            String vnp_TxnRef = fields.get("vnp_TxnRef");
            String vnp_TransactionNo = fields.get("vnp_TransactionNo");
            String vnp_Amount = fields.get("vnp_Amount");

            PaymentStatus status = "00".equals(vnp_ResponseCode) ?
                    PaymentStatus.COMPLETED : PaymentStatus.FAILED;

            BigDecimal amount = new BigDecimal(vnp_Amount).divide(BigDecimal.valueOf(100));

            return PaymentVerificationResult.builder()
                    .valid(true)
                    .transactionId(vnp_TxnRef)
                    .gatewayTransactionId(vnp_TransactionNo)
                    .amount(amount)
                    .status(status)
                    .message(getResponseMessage(vnp_ResponseCode))
                    .responseData(fields.toString())
                    .build();

        } catch (Exception e) {
            log.error("Error verifying VNPay payment", e);
            throw new CustomException(ErrorCode.VNPAY_SIGNATURE_VERIFICATION_FAILED, e);
        }
    }

    @Override
    public boolean supports(PaymentMethod paymentMethod) {
        return PaymentMethod.VNPAY.equals(paymentMethod);
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
