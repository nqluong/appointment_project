package org.project.appointment_project.schedule.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.schedule.dto.request.SlotGenerationRequest;
import org.project.appointment_project.schedule.dto.response.SlotGenerationResponse;
import org.project.appointment_project.schedule.repository.SlotGenerationRepository;
import org.project.appointment_project.schedule.service.SlotGenerationService;
import org.project.appointment_project.schedule.service.SlotGenerationValidationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SlotGenerationServiceImpl implements SlotGenerationService {

    SlotGenerationValidationService validationService;
    SlotGenerationRepository slotGenerationRepository;

    @Override
    @Transactional
    public SlotGenerationResponse generateSlots(SlotGenerationRequest request) {
        validationService.validateRequest(request);

        slotGenerationRepository.generateSlotsForRange(request.getDoctorId(),
                request.getStartDate(), request.getEndDate());
        long totalSlots = slotGenerationRepository.countAvailableSlots(
                request.getDoctorId(), request.getStartDate(), request.getEndDate()
        );

        return SlotGenerationResponse.builder()
                .doctorId(request.getDoctorId())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .totalSlotsGenerated((int) totalSlots)
                .message("Slots generated successfully")
                .build();
    }
}
