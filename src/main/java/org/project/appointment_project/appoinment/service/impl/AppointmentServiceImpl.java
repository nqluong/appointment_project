package org.project.appointment_project.appoinment.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.appoinment.dto.request.CreateAppointmentRequest;
import org.project.appointment_project.appoinment.dto.response.AppointmentResponse;
import org.project.appointment_project.appoinment.enums.Status;
import org.project.appointment_project.appoinment.mapper.AppointmentMapper;
import org.project.appointment_project.appoinment.model.Appointment;
import org.project.appointment_project.appoinment.repository.AppointmentRepository;
import org.project.appointment_project.appoinment.service.AppointmentService;
import org.project.appointment_project.appoinment.service.AppointmentValidationService;
import org.project.appointment_project.common.dto.PageResponse;
import org.project.appointment_project.common.exception.CustomException;
import org.project.appointment_project.common.exception.ErrorCode;
import org.project.appointment_project.common.mapper.PageMapper;
import org.project.appointment_project.schedule.model.DoctorAvailableSlot;
import org.project.appointment_project.schedule.repository.DoctorAvailableSlotRepository;
import org.project.appointment_project.schedule.service.SlotStatusService;
import org.project.appointment_project.user.model.User;
import org.project.appointment_project.user.model.UserRole;
import org.project.appointment_project.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AppointmentServiceImpl implements AppointmentService {

    AppointmentRepository appointmentRepository;
    DoctorAvailableSlotRepository slotRepository;
    UserRepository userRepository;
    AppointmentValidationService validationService;
    PageMapper pageMapper;
    AppointmentMapper appointmentMapper;
    SlotStatusService slotStatusService;

    //Tạo lịch hẹn mới vả xử lý transaction, lock
    @Transactional
    @Override
    public AppointmentResponse createAppointment(CreateAppointmentRequest request) {
        log.info("Creating appointment for patient {} with doctor {} at slot {}",
                request.getPatientId(), request.getDoctorId(), request.getSlotId());

        try {
            //Lấy và lock slot để tránh concurrent booking
            DoctorAvailableSlot slot = getAndLockSlot(request.getSlotId());

            validationService.validateSlotForBooking(slot, request.getDoctorId());

            User patient = getUser(request.getPatientId(), ErrorCode.PATIENT_NOT_FOUND);
            User doctor = getUser(request.getDoctorId(), ErrorCode.DOCTOR_NOT_FOUND);

            validationService.validatePatientForBooking(patient, slot);
            validationService.validateDoctorForBooking(doctor);

            if (!slot.isAvailable()) {
                throw new CustomException(ErrorCode.SLOT_ALREADY_BOOKED);
            }

            Appointment appointment = createAppointmentEntity(request, doctor, patient, slot);
            appointment = appointmentRepository.save(appointment);

            // Cập nhật slot status
            slotStatusService.reserveSlot(slot.getId());

            log.info("Successfully created appointment {} for patient {} with doctor {}",
                    appointment.getId(), patient.getId(), doctor.getId());

            return appointmentMapper.toResponse(appointment);

        } catch (CustomException e) {
            log.error("Appointment creation failed: {} - {}", e.getErrorCode().getCode(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during appointment creation", e);
            throw new CustomException(ErrorCode.APPOINTMENT_CREATION_FAILED);
        }
    }

    @Override
    public PageResponse<AppointmentResponse> getAppointments(UUID userId, Status status, Pageable pageable) {
        try {
            if (userId != null) {
                validateUserExists(userId);
            }

            Page<Appointment> appointmentsPage = getAppointmentsPage(userId, status, pageable);

            return pageMapper.toPageResponse(appointmentsPage, appointmentMapper::toResponse);

        } catch (CustomException e) {
            log.error("Failed to get appointments: {} - {}", e.getErrorCode().getCode(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error while getting appointments", e);
            throw new CustomException(ErrorCode.APPOINTMENT_FETCH_FAILED);
        }
    }

    @Override
    @Transactional
    public AppointmentResponse updateAppointmentStatus(UUID appointmentId, Status newStatus) {
        try {
            // Lấy appointment với lock
            Appointment appointment = getAppointmentWithLock(appointmentId);

            validateStatusTransition(appointment.getStatus(), newStatus);

            // Cập nhật status
            appointment.setStatus(newStatus);
            appointment = appointmentRepository.save(appointment);

            handleStatusSideEffects(appointment, newStatus);

            log.info("Successfully updated appointment {} status to {}", appointmentId, newStatus);
            return appointmentMapper.toResponse(appointment);

        } catch (CustomException e) {
            log.error("Failed to update appointment status: {} - {}", e.getErrorCode().getCode(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error while updating appointment status", e);
            throw new CustomException(ErrorCode.APPOINTMENT_STATUS_UPDATE_FAILED);
        }
    }

    @Transactional
    @Override
    public AppointmentResponse completeAppointment(UUID appointmentId) {
        try {
            Appointment appointment = getAppointmentWithLock(appointmentId);

            // Validate có thể complete không
            validateAppointmentCanBeCompleted(appointment);

            // Cập nhật status thành COMPLETED
            appointment.setStatus(Status.COMPLETED);
            appointment = appointmentRepository.save(appointment);


            log.info("Successfully completed appointment {}", appointmentId);
            return appointmentMapper.toResponse(appointment);

        } catch (CustomException e) {
            log.error("Failed to complete appointment: {} - {}", e.getErrorCode().getCode(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error while completing appointment", e);
            throw new CustomException(ErrorCode.APPOINTMENT_COMPLETION_FAILED);
        }
    }

    @Override
    @Transactional
    public AppointmentResponse cancelAppointment(UUID appointmentId, String reason) {
        try {
            Appointment appointment = getAppointmentWithLock(appointmentId);

            // Validate có thể cancel không
            validateAppointmentCanBeCancelled(appointment);

            appointment.setStatus(Status.CANCELLED);
            if (reason != null && !reason.trim().isEmpty()) {
                String currentNotes = appointment.getNotes();
                String cancelReason = "CANCELLED: " + reason.trim();
                appointment.setNotes(currentNotes == null ? cancelReason : currentNotes + " | " + cancelReason);
            }

            appointment = appointmentRepository.save(appointment);

            // Giải phóng slot để có thể book lại
            if (appointment.getSlot() != null) {
                slotStatusService.releaseSlot(appointment.getSlot().getId());
            }

            log.info("Successfully cancelled appointment {}", appointmentId);
            return appointmentMapper.toResponse(appointment);

        } catch (CustomException e) {
            log.error("Failed to cancel appointment: {} - {}", e.getErrorCode().getCode(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error while cancelling appointment", e);
            throw new CustomException(ErrorCode.APPOINTMENT_CANCELLATION_FAILED);
        }
    }

    //Lấy và lock slot để tránh concurrent access
    private DoctorAvailableSlot getAndLockSlot(UUID slotId) {
        return slotRepository.findByIdWithLock(slotId)
                .orElseThrow(() -> new CustomException(ErrorCode.SLOT_NOT_FOUND));
    }

     //Lấy thông tin user
    private User getUser(UUID userId, ErrorCode errorCode) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(errorCode));
    }


    //Tạo entity Appointment từ request
    private Appointment createAppointmentEntity(CreateAppointmentRequest request,
                                                User doctor, User patient, DoctorAvailableSlot slot) {
        return Appointment.builder()
                .doctor(doctor)
                .patient(patient)
                .slot(slot)
                .consultationFee(doctor.getMedicalProfile().getConsultationFee())
                .appointmentDate(slot.getSlotDate())
                .status(Status.PENDING)
                .notes(request.getNotes())
                .build();
    }

    private Appointment getAppointmentWithLock(UUID appointmentId) {
        return appointmentRepository.findByIdWithLock(appointmentId)
                .orElseThrow(() -> new CustomException(ErrorCode.APPOINTMENT_NOT_FOUND));
    }

    private void validateStatusTransition(Status currentStatus, Status newStatus) {
        if (currentStatus == newStatus) {
            throw new CustomException(ErrorCode.APPOINTMENT_STATUS_ALREADY_SET);
        }

        boolean isValidTransition = switch (currentStatus) {
            case PENDING -> newStatus == Status.CONFIRMED || newStatus == Status.CANCELLED;
            case CONFIRMED -> newStatus == Status.COMPLETED || newStatus == Status.CANCELLED;
            case COMPLETED -> false;
            case CANCELLED -> false;
            default -> false;
        };

        if (!isValidTransition) {
            log.warn("Invalid status transition from {} to {}", currentStatus, newStatus);
            throw new CustomException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
    }

    private void validateAppointmentCanBeCompleted(Appointment appointment) {
        // Chỉ appointment CONFIRMED mới có thể complete
        if (appointment.getStatus() != Status.CONFIRMED) {
            throw new CustomException(ErrorCode.APPOINTMENT_NOT_CONFIRMED);
        }

        // - Kiểm tra appointment date đã qua chưa

        if (appointment.getDoctorNotes() == null || appointment.getDoctorNotes().trim().isEmpty()) {
            throw new CustomException(ErrorCode.DOCTOR_NOTES_REQUIRED);
        }
    }

    private void validateAppointmentCanBeCancelled(Appointment appointment) {
        if (appointment.getStatus() == Status.COMPLETED) {
            throw new CustomException(ErrorCode.APPOINTMENT_ALREADY_COMPLETED);
        }

        if (appointment.getStatus() == Status.CANCELLED) {
            throw new CustomException(ErrorCode.APPOINTMENT_ALREADY_CANCELLED);
        }
    }

    private void handleStatusSideEffects(Appointment appointment, Status newStatus) {
        switch (newStatus) {
            case CONFIRMED -> {
                log.debug("Appointment {} confirmed, sending notifications", appointment.getId());
            }
            case COMPLETED -> {
                log.debug("Appointment {} completed, slot released", appointment.getId());
            }
            case CANCELLED -> {
                if (appointment.getSlot() != null) {
                    slotStatusService.releaseSlot(appointment.getSlot().getId());
                }
                log.debug("Appointment {} cancelled, slot released", appointment.getId());
            }
            default -> {
                // No side effects for other statuses
            }
        }
    }

    private void validateUserExists(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }
    }

    private Page<Appointment> getAppointmentsPage(UUID userId, Status status, Pageable pageable) {

        if (userId == null) {
            log.debug("Fetching all appointments for admin with status filter: {}", status);
            return appointmentRepository.findAllAppointmentsByStatus(status, pageable);
        } else {
            log.debug("Fetching appointments for user {} with status filter: {}", userId, status);
            return appointmentRepository.findAppointmentsByUserIdAndStatus(userId, status, pageable);
        }
    }

}
