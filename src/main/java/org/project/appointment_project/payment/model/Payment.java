package org.project.appointment_project.payment.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.project.appointment_project.appoinment.model.Appointment;
import org.project.appointment_project.payment.enums.PaymentMethod;
import org.project.appointment_project.payment.enums.PaymentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @NotNull(message = "Appointment is required")
    @ManyToOne
    @JoinColumn(name = "appointment_id")
    Appointment appointment;

    @Min(value = 0, message = "Amount must be non-negative")
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    Double amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status")
    PaymentStatus paymentStatus;

    @Column(name = "transaction_id")
    String transactionId;

    @Column(name = "notes")
    String notes;

    @Column(name = "payment_date")
    LocalDateTime paymentDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;
}
