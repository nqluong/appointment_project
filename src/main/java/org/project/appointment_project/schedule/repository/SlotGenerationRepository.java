package org.project.appointment_project.schedule.repository;

import java.time.LocalDate;
import java.util.UUID;

public interface SlotGenerationRepository {

    void generateSlotsForRange(UUID doctorId, LocalDate startDate, LocalDate endDate);

    long countAvailableSlots(UUID doctorId, LocalDate startDate, LocalDate endDate);
}
