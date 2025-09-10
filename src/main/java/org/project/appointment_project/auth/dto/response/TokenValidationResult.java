package org.project.appointment_project.auth.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.project.appointment_project.user.enums.TokenType;
import org.project.appointment_project.user.model.User;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TokenValidationResult {
    String tokenHash;
    UUID userId;
    User user;
    LocalDateTime expirationTime;
    TokenType tokenType;

}
