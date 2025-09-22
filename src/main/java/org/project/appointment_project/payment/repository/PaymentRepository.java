package org.project.appointment_project.payment.repository;

import org.project.appointment_project.payment.enums.PaymentStatus;
import org.project.appointment_project.payment.enums.PaymentType;
import org.project.appointment_project.payment.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByTransactionId(String transactionId);

    boolean existsByAppointmentIdAndPaymentStatusIn(UUID appointmentId, List<PaymentStatus> statuses);

    boolean existsByAppointmentIdAndPaymentTypeAndPaymentStatusIn(
            UUID appointmentId, PaymentType paymentType, List<PaymentStatus> statuses);

    @Query("SELECT p FROM Payment p WHERE p.appointment.id = :appointmentId " +
            "AND p.paymentStatus IN :statuses " +
            "AND p.paymentType IS NOT NULL")
    List<Payment> findValidPaymentsByAppointmentIdAndStatus(
            @Param("appointmentId") UUID appointmentId,
            @Param("statuses") List<PaymentStatus> statuses);


    @Query("SELECT p FROM Payment p WHERE p.paymentStatus = :status " +
            "AND p.createdAt < :createdBefore " +
            "AND p.createdAt >= :safetyDate")
    List<Payment> findByStatusAndCreatedAtBeforeWithSafety(
            @Param("status") PaymentStatus status,
            @Param("createdBefore") LocalDateTime createdBefore,
            @Param("safetyDate") LocalDateTime safetyDate
    );

    // Đếm số lượng payment cũ để kiểm tra
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.paymentStatus = :status " +
            "AND p.createdAt < :cutoffDate")
    long countOldPendingPayments(@Param("status") PaymentStatus status,
                                 @Param("cutoffDate") LocalDateTime cutoffDate);
}
