package org.project.appointment_project.payment.service.impl;

import org.project.appointment_project.payment.enums.PaymentType;
import org.project.appointment_project.payment.service.OrderInfoBuilder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class OrderInfoBuilderImpl implements OrderInfoBuilder {

    @Override
    public String buildOrderInfo(PaymentType paymentType, UUID appointmentId) {
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
}
