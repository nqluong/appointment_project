package org.project.appointment_project.schedule.service;

import org.project.appointment_project.schedule.dto.request.BatchSlotStatusRequest;
import org.project.appointment_project.schedule.model.DoctorAvailableSlot;

import java.util.List;
import java.util.UUID;

public interface SlotStatusValidationService {
    /**
     * Validate single slot status update request
     */
    void validateSlotAvailabilityUpdate(UUID slotId, DoctorAvailableSlot slot, boolean newStatus);

    /**
     * Validate multiple slot status update requests
     */
    void validateMultipleSlotStatusUpdate(List<BatchSlotStatusRequest> requests);

    /**
     * Validate slot reservation
     */
    void validateSlotReservation(UUID slotId, DoctorAvailableSlot slot);

    /**
     * Validate slot release
     */
    void validateSlotRelease(UUID slotId, DoctorAvailableSlot slot);

    /**
     * Common validations for slot operations
     */
    void validateSlotOperation(UUID slotId, DoctorAvailableSlot slot);
}
