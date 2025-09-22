package org.project.appointment_project.payment.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.project.appointment_project.payment.enums.PaymentMethod;
import org.project.appointment_project.payment.enums.PaymentType;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreatePaymentRequest {
    @NotNull(message = "Appointment ID is required")
    UUID appointmentId;

    @NotNull(message = "Payment type is required")
    PaymentType paymentType;

    @NotNull(message = "Payment method is required")
    PaymentMethod paymentMethod;

    String notes;

}
