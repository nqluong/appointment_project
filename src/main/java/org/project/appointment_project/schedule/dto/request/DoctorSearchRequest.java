package org.project.appointment_project.schedule.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DoctorSearchRequest {
    UUID specialtyId;
    String doctorName;
    LocalDate availableDate;
    LocalTime preferredStartTime;
    LocalTime preferredEndTime;
    String location;
    Boolean isApproved;
    Integer minExperience;
    Integer maxExperience;
    String qualification;
    int page = 0;
    int size = 20;
    String sortBy = "createdAt";
    String sortDirection = "desc";
}
