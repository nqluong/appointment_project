package org.project.appointment_project.schedule.service;

import org.project.appointment_project.schedule.dto.request.ScheduleEntryRequest;

import java.util.List;

public interface ScheduleValidationService {
    void validateScheduleEntries(List<ScheduleEntryRequest> scheduleEntries);
    void validateScheduleEntry(ScheduleEntryRequest entry);
}
