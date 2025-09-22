package org.project.appointment_project.payment.service.impl;

import org.project.appointment_project.payment.service.TransactionIdGenerator;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TransactionIdGeneratorImpl implements TransactionIdGenerator {
    private static final String PREFIX = "TXN";

    @Override
    public String generateTransactionId() {
        return PREFIX + System.currentTimeMillis() +
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
