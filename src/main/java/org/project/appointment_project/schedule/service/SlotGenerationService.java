package org.project.appointment_project.schedule.service;

import org.project.appointment_project.schedule.dto.request.SlotGenerationRequest;
import org.project.appointment_project.schedule.dto.response.SlotGenerationResponse;

public interface SlotGenerationService {

    SlotGenerationResponse generateSlots(SlotGenerationRequest request);
}
