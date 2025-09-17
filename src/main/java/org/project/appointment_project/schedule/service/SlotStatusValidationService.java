package org.project.appointment_project.schedule.service;

import org.project.appointment_project.schedule.dto.request.BatchSlotStatusRequest;
import org.project.appointment_project.schedule.model.DoctorAvailableSlot;

import java.util.List;
import java.util.UUID;

public interface SlotStatusValidationService {

    //Validate status của một slot update
    void validateSlotAvailabilityUpdate(UUID slotId, DoctorAvailableSlot slot, boolean newStatus);

    //Validate status của một slot update
    void validateMultipleSlotStatusUpdate(List<BatchSlotStatusRequest> requests);

    //Validate đóng của một slot
    void validateSlotReservation(UUID slotId, DoctorAvailableSlot slot);

    //Validate mở slot
    void validateSlotRelease(UUID slotId, DoctorAvailableSlot slot);

    // Validate chung cho các slot
    void validateSlotOperation(UUID slotId, DoctorAvailableSlot slot);
}
