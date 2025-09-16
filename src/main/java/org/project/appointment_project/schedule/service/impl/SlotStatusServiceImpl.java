package org.project.appointment_project.schedule.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.common.exception.CustomException;
import org.project.appointment_project.common.exception.ErrorCode;
import org.project.appointment_project.schedule.dto.request.BatchSlotStatusRequest;
import org.project.appointment_project.schedule.dto.response.SlotStatusUpdateResponse;
import org.project.appointment_project.schedule.model.DoctorAvailableSlot;
import org.project.appointment_project.schedule.repository.SlotStatusRepository;
import org.project.appointment_project.schedule.service.SlotStatusService;
import org.project.appointment_project.schedule.service.SlotStatusValidationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SlotStatusServiceImpl implements SlotStatusService {

    SlotStatusRepository slotStatusRepository;
    SlotStatusValidationService slotStatusValidationService;

    @Override
    public SlotStatusUpdateResponse markSlotAvailable(UUID slotId) {
        return updateSlotStatus(slotId, true, "Slot marked as available");
    }

    @Override
    public SlotStatusUpdateResponse markSlotUnavailable(UUID slotId) {
        return updateSlotStatus(slotId, false, "Slot marked as unavailable");
    }


    @Override
    @Transactional
    public List<SlotStatusUpdateResponse> updateMultipleSlotStatus(List<BatchSlotStatusRequest> requests) {
        slotStatusValidationService.validateMultipleSlotStatusUpdate(requests);

        return requests.stream()
                .map(request -> updateSlotStatus(
                        request.getSlotId(),
                        request.getIsAvailable(),
                        request.getReason() != null ? request.getReason() :
                                (request.getIsAvailable() ? "Batch update: available" : "Batch update: unavailable")
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SlotStatusUpdateResponse reserveSlot(UUID slotId) {
        DoctorAvailableSlot slot = findSlotWithDoctor(slotId);
        slotStatusValidationService.validateSlotReservation(slotId, slot);

        return updateSlotStatus(slotId, false, "Reserved for appointment booking");
    }

    @Override
    @Transactional
    public SlotStatusUpdateResponse releaseSlot(UUID slotId) {

        DoctorAvailableSlot slot = findSlotWithDoctor(slotId);
        slotStatusValidationService.validateSlotRelease(slotId, slot);

        return updateSlotStatus(slotId, true, "Released from reservation");
    }

    private SlotStatusUpdateResponse updateSlotStatus(UUID slotId, boolean isAvailable, String reason) {
        DoctorAvailableSlot slot = findSlotWithDoctor(slotId);

        slotStatusValidationService.validateSlotAvailabilityUpdate(slotId, slot, isAvailable);

        slot.setAvailable(isAvailable);
        DoctorAvailableSlot updatedSlot = slotStatusRepository.save(slot);


        return SlotStatusUpdateResponse.builder()
                .slotId(updatedSlot.getId())
                .doctorId(updatedSlot.getDoctor().getId())
                .slotDate(updatedSlot.getSlotDate())
                .startTime(updatedSlot.getStartTime())
                .endTime(updatedSlot.getEndTime())
                .isAvailable(updatedSlot.isAvailable()) // This should now reflect the updated state
                .message(reason)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private DoctorAvailableSlot findSlotWithDoctor(UUID slotId) {
        return slotStatusRepository.findByIdWithDoctor(slotId)
                .orElseThrow(() -> new CustomException(ErrorCode.SLOT_NOT_FOUND));
    }

}
