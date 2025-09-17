package org.project.appointment_project.schedule.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.project.appointment_project.user.model.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "doctor_available_slots")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@ToString(exclude = {"doctor"})
@EqualsAndHashCode(exclude = {"doctor"})
public class DoctorAvailableSlot {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @NotNull(message = "Doctor user ID is required")
    @ManyToOne
    @JoinColumn(name = "doctor_user_id", nullable = false)
    User doctor;

    @NotNull(message = "Slot date is required")
    @Column(name = "slot_date", nullable = false)
    LocalDate slotDate;

    @NotNull(message = "Start time is required")
    @Column(name = "start_time", nullable = false)
    LocalTime startTime;

    @NotNull(message = "End time is required")
    @Column(name = "end_time", nullable = false)
    LocalTime endTime;

    @Column(name = "is_available", nullable = false)
    boolean isAvailable = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;
}
