package org.project.appointment_project.appoinment.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateAppointmentRequest {
    @NotNull(message = "ID bác sĩ không được để trống")
    UUID doctorId;

    @NotNull(message = "ID slot không được để trống")
    UUID slotId;

    @NotNull(message = "ID bệnh nhân không được để trống")
    UUID patientId;

    @Size(max = 500, message = "Ghi chú không được vượt quá 500 ký tự")
    String notes;
}
