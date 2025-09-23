package org.project.appointment_project.schedule.validator;

import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.common.exception.CustomException;
import org.project.appointment_project.common.exception.ErrorCode;
import org.project.appointment_project.schedule.dto.request.CreateAbsenceRequest;
import org.project.appointment_project.schedule.dto.request.UpdateAbsenceRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;

@Component
@Slf4j
public class AbsenceValidator {
    public void validateCreateRequest(CreateAbsenceRequest request) {
        validateAbsenceDate(request.getAbsenceDate());
        validateTimeRange(request.getStartTime(), request.getEndTime());
    }

    public void validateUpdateRequest(UpdateAbsenceRequest request) {
        if (request.getAbsenceDate() != null) {
            validateAbsenceDate(request.getAbsenceDate());
        }
        validateTimeRange(request.getStartTime(), request.getEndTime());
    }

    private void validateAbsenceDate(LocalDate absenceDate) {
        if (absenceDate != null && absenceDate.isBefore(LocalDate.now())) {
            throw new CustomException(ErrorCode.ABSENCE_PAST_DATE);
        }
    }

    private void validateTimeRange(LocalTime startTime, LocalTime endTime) {

        if (startTime != null && endTime != null) {
            if (startTime.isAfter(endTime) || startTime.equals(endTime)) {
                throw new CustomException(ErrorCode.INVALID_TIME_RANGE);
            }
        }
        else if ((startTime != null && endTime == null) || (startTime == null && endTime != null)) {
            throw new CustomException(ErrorCode.ABSENCE_TIME_MISMATCH);
        }
    }
}
