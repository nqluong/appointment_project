package org.project.appointment_project.payment.service;

import org.project.appointment_project.payment.dto.request.PaymentRefundRequest;
import org.project.appointment_project.payment.model.Payment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public interface RefundPolicyService {

    // Tính toán số tiền có thể hoàn
    BigDecimal calculateRefundAmount(Payment payment, PaymentRefundRequest request);

    //Tính phần trăm hoàn tiền dựa trên thời gian hủy
    BigDecimal calculateRefundPercentage(LocalDate appointmentDate, LocalDateTime cancellationDateTime);

}
