package org.project.appointment_project.schedule.service;

import org.project.appointment_project.schedule.dto.request.SlotGenerationRequest;

public interface SlotGenerationValidationService {

    void validateRequest(SlotGenerationRequest request);
}
