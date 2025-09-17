package org.project.appointment_project.appoinment.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.project.appointment_project.appoinment.enums.Status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AppointmentResponse {
    UUID appointmentId;
    UUID doctorId;
    String doctorName;
    String specialtyName;
    UUID patientId;
    String patientName;
    LocalDate appointmentDate;
    LocalTime startTime;
    LocalTime endTime;
    BigDecimal consultationFee;
    Status status;
    String notes;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
