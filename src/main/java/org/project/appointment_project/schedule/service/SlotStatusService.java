package org.project.appointment_project.schedule.service;

import org.project.appointment_project.schedule.dto.request.BatchSlotStatusRequest;
import org.project.appointment_project.schedule.dto.response.SlotStatusUpdateResponse;

import java.util.List;
import java.util.UUID;

public interface SlotStatusService {

    /**
     * Cập nhật trạng thái của một slot
     */
    SlotStatusUpdateResponse markSlotAvailable(UUID slotId);

    //Đánh dấu slot là unavailable
    SlotStatusUpdateResponse markSlotUnavailable(UUID slotId);

    /**
     * Cập nhật trạng thái nhiều slots cùng lúc
     */
    List<SlotStatusUpdateResponse> updateMultipleSlotStatus(List<BatchSlotStatusRequest> requests);

    /**
     * Đánh dấu slot là không khả dụng (reserved for booking)
     */
    SlotStatusUpdateResponse reserveSlot(UUID slotId);

    /**
     * Giải phóng slot (make available again)
     */
    SlotStatusUpdateResponse releaseSlot(UUID slotId);

}
