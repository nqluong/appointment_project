package org.project.appointment_project.user.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AssignRoleRequest {
     UUID userId;
     UUID roleId;
     LocalDateTime expiresAt;
}
