package org.project.appointment_project.notification.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.project.appointment_project.notification.enums.Status;
import org.project.appointment_project.notification.enums.Type;
import org.project.appointment_project.user.model.User;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    User user;

    @NotBlank(message = "Title is required")
    @Column(name = "title", nullable = false)
    String title;

    @NotBlank(message = "Message is required")
    @Column(name = "message", nullable = false)
    String message;

    @NotNull(message = "Type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    Type type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    Status status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @Column(name = "sent_at")
    LocalDateTime sentAt;
}
