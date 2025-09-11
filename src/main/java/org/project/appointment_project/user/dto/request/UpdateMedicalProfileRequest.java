package org.project.appointment_project.user.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateMedicalProfileRequest {
    String bloodType;
    String allergies;
    String medicalHistory;
    String emergencyContactName;
    String emergencyContactPhone;
}
