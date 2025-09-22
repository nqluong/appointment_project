package org.project.appointment_project.payment.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.appoinment.model.Appointment;
import org.project.appointment_project.common.exception.CustomException;
import org.project.appointment_project.common.exception.ErrorCode;
import org.project.appointment_project.payment.enums.PaymentType;
import org.project.appointment_project.payment.service.PaymentAmountCalculator;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@Slf4j
public class PaymentAmountCalculatorImpl implements PaymentAmountCalculator {

    private static final BigDecimal DEPOSIT_RATE = BigDecimal.valueOf(0.3);
    private static final BigDecimal REMAINING_RATE = BigDecimal.valueOf(0.7);

    @Override
    public BigDecimal calculatePaymentAmount(Appointment appointment, PaymentType paymentType) {
        BigDecimal consultationFee = appointment.getConsultationFee();

        switch (paymentType) {
            case DEPOSIT:
                return consultationFee.multiply(DEPOSIT_RATE)
                        .setScale(2, RoundingMode.HALF_UP);
            case FULL:
                return consultationFee;
            case REMAINING:
                return consultationFee.multiply(REMAINING_RATE)
                        .setScale(2, RoundingMode.HALF_UP);
            default:
                throw new CustomException(ErrorCode.INVALID_PAYMENT_TYPE);
        }
    }
}
