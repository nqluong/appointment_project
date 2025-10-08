package org.project.appointment_project.schedule.dto.cache;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DoctorAvailabilityCacheData implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    UUID doctorId;
    String date;
    List<TimeSlot> slots;
    Integer totalSlots;
}
