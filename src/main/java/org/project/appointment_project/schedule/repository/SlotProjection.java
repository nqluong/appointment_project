package org.project.appointment_project.schedule.repository;

import java.time.LocalDate;
import java.time.LocalTime;

public interface SlotProjection {
    String getSlotId();
    LocalDate getSlotDate();
    LocalTime getStartTime();
    LocalTime getEndTime();
    Boolean getIsAvailable();
}
