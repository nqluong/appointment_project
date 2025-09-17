package org.project.appointment_project.appoinment.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.appoinment.dto.request.CreateAppointmentRequest;
import org.project.appointment_project.appoinment.dto.response.AppointmentResponse;
import org.project.appointment_project.appoinment.enums.Status;
import org.project.appointment_project.appoinment.model.Appointment;
import org.project.appointment_project.appoinment.repository.AppointmentRepository;
import org.project.appointment_project.appoinment.service.AppointmentService;
import org.project.appointment_project.appoinment.service.AppointmentValidationService;
import org.project.appointment_project.common.exception.CustomException;
import org.project.appointment_project.common.exception.ErrorCode;
import org.project.appointment_project.schedule.model.DoctorAvailableSlot;
import org.project.appointment_project.schedule.repository.DoctorAvailableSlotRepository;
import org.project.appointment_project.user.model.User;
import org.project.appointment_project.user.repository.UserRepository;
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

    /**
     * Tạo lịch hẹn mới với xử lý transaction và locking
     */
    @Transactional
    @Override
    public AppointmentResponse createAppointment(CreateAppointmentRequest request) {
        log.info("Creating appointment for patient {} with doctor {} at slot {}",
                request.getPatientId(), request.getDoctorId(), request.getSlotId());

        try {
            //Lấy và lock slot để tránh concurrent booking
            DoctorAvailableSlot slot = getAndLockSlot(request.getSlotId());

            // Validate slot
            validationService.validateSlotForBooking(slot, request.getDoctorId());

            // Lấy thông tin bệnh nhân và bác sĩ
            User patient = getUser(request.getPatientId(), ErrorCode.PATIENT_NOT_FOUND);
            User doctor = getUser(request.getDoctorId(), ErrorCode.DOCTOR_NOT_FOUND);

            //Validate bệnh nhân và bác sĩ
            validationService.validatePatientForBooking(patient, slot);
            validationService.validateDoctorForBooking(doctor);

            // Kiểm tra lại slot availability sau khi lock (double-check)
            if (!slot.isAvailable()) {
                throw new CustomException(ErrorCode.SLOT_ALREADY_BOOKED);
            }

            // Tạo appointment
            Appointment appointment = createAppointmentEntity(request, doctor, patient, slot);
            appointment = appointmentRepository.save(appointment);
            appointmentRepository.flush();

            // Cập nhật slot status
            updateSlotStatus(slot, false);
            slotRepository.flush();

            log.info("Successfully created appointment {} for patient {} with doctor {}",
                    appointment.getId(), patient.getId(), doctor.getId());

            return buildAppointmentResponse(appointment, slot);

        } catch (CustomException e) {
            log.error("Appointment creation failed: {} - {}", e.getErrorCode().getCode(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during appointment creation", e);
            throw new CustomException(ErrorCode.APPOINTMENT_CREATION_FAILED);
        }
    }

    /**
     * Lấy và lock slot để tránh concurrent access
     */
    private DoctorAvailableSlot getAndLockSlot(UUID slotId) {
        return slotRepository.findByIdWithLock(slotId)
                .orElseThrow(() -> new CustomException(ErrorCode.SLOT_NOT_FOUND));
    }

    /**
     * Lấy thông tin user với error handling
     */
    private User getUser(UUID userId, ErrorCode errorCode) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(errorCode));
    }

    /**
     * Tạo entity Appointment từ request
     */
    private Appointment createAppointmentEntity(CreateAppointmentRequest request,
                                                User doctor, User patient, DoctorAvailableSlot slot) {
        return Appointment.builder()
                .doctor(doctor)
                .patient(patient)
                .slot(slot)
                .appointmentDate(slot.getSlotDate())
                .status(Status.PENDING)
                .notes(request.getNotes())
                .build();
    }

    /**
     * Cập nhật trạng thái slot
     */
    private void updateSlotStatus(DoctorAvailableSlot slot, boolean isAvailable) {
        slot.setAvailable(isAvailable);
        slotRepository.save(slot);
    }

    /**
     * Build response từ appointment entity
     */
    private AppointmentResponse buildAppointmentResponse(Appointment appointment, DoctorAvailableSlot slot) {
        User doctor = appointment.getDoctor();
        User patient = appointment.getPatient();

        String doctorName = String.format("%s %s",
                doctor.getUserProfile().getFirstName(),
                doctor.getUserProfile().getLastName()).trim();

        String patientName = String.format("%s %s",
                patient.getUserProfile().getFirstName(),
                patient.getUserProfile().getLastName()).trim();

        String specialtyName = doctor.getMedicalProfile() != null
                && doctor.getMedicalProfile().getSpecialty() != null
                ? doctor.getMedicalProfile().getSpecialty().getName()
                : null;

        return AppointmentResponse.builder()
                .appointmentId(appointment.getId())
                .doctorId(doctor.getId())
                .doctorName(doctorName)
                .specialtyName(specialtyName)
                .patientId(patient.getId())
                .patientName(patientName)
                .appointmentDate(slot.getSlotDate())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .status(appointment.getStatus())
                .notes(appointment.getNotes())
                .createdAt(appointment.getCreatedAt())
                .updatedAt(appointment.getUpdatedAt())
                .build();
    }
}
