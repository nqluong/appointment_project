package org.project.appointment_project.appoinment.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.project.appointment_project.appoinment.enums.Status;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateAppointmentStatusRequest {

    @NotNull(message = "Status is required")
    Status status;
}
