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


    //Cập nhật trạng thái slot
    private void updateSlotStatus(DoctorAvailableSlot slot, boolean isAvailable) {
        slot.setAvailable(isAvailable);
        slotRepository.save(slot);
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
