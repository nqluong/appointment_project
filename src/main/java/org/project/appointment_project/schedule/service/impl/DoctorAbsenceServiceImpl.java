package org.project.appointment_project.schedule.service.impl;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.project.appointment_project.appoinment.enums.Status;
import org.project.appointment_project.appoinment.model.Appointment;
import org.project.appointment_project.appoinment.repository.AppointmentRepository;
import org.project.appointment_project.common.dto.PageResponse;
import org.project.appointment_project.common.exception.CustomException;
import org.project.appointment_project.common.exception.ErrorCode;
import org.project.appointment_project.common.mapper.PageMapper;
import org.project.appointment_project.common.util.SecurityUtils;
import org.project.appointment_project.payment.dto.request.PaymentRefundRequest;
import org.project.appointment_project.payment.enums.PaymentStatus;
import org.project.appointment_project.payment.enums.RefundType;
import org.project.appointment_project.payment.model.Payment;
import org.project.appointment_project.payment.repository.PaymentRepository;
import org.project.appointment_project.payment.service.PaymentService;
import org.project.appointment_project.schedule.dto.request.CreateAbsenceRequest;
import org.project.appointment_project.schedule.dto.request.UpdateAbsenceRequest;
import org.project.appointment_project.schedule.dto.response.DoctorAbsenceResponse;
import org.project.appointment_project.schedule.mapper.DoctorAbsenceMapper;
import org.project.appointment_project.schedule.model.DoctorAbsence;
import org.project.appointment_project.schedule.repository.DoctorAbsenceRepository;
import org.project.appointment_project.schedule.service.DoctorAbsenceService;
import org.project.appointment_project.schedule.service.SlotStatusService;
import org.project.appointment_project.schedule.validator.AbsenceValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DoctorAbsenceServiceImpl implements DoctorAbsenceService {

    DoctorAbsenceRepository doctorAbsenceRepository;
    DoctorAbsenceMapper doctorAbsenceMapper;
    AbsenceValidator absenceValidator;
    PageMapper pageMapper;
    SecurityUtils securityUtils;

    AppointmentRepository appointmentRepository;
    PaymentRepository paymentRepository;
    PaymentService paymentService;
    SlotStatusService slotStatusService;

    /**
     - Tạo lịch nghỉ mới cho bác sĩ
     - Tạo và lưu lịch nghỉ mới
     - Xử lý các lịch hẹn bị ảnh hưởng
     */
    @Override
    public DoctorAbsenceResponse createAbsence(CreateAbsenceRequest request) {
        absenceValidator.validateCreateRequest(request);

        // Kiểm tra xung đột với lịch nghỉ hiện có
        if (hasConflictingAbsence(request.getDoctorUserId(), request.getAbsenceDate(),
                request.getStartTime(), request.getEndTime(), null)) {
            throw new CustomException(ErrorCode.ABSENCE_CONFLICT);
        }

        // Tạo và lưu lịch nghỉ mới
        DoctorAbsence absence = doctorAbsenceMapper.toEntity(request);
        DoctorAbsence savedAbsence = doctorAbsenceRepository.save(absence);

        log.info("Đã tạo lịch nghỉ mới với ID: {}", savedAbsence.getId());
        handleAffectedAppointmentsAsync(savedAbsence);
        return doctorAbsenceMapper.toDto(savedAbsence);
    }

    @Override
    public DoctorAbsenceResponse updateAbsence(UUID absenceId, UpdateAbsenceRequest request) {
        absenceValidator.validateUpdateRequest(request);

        DoctorAbsence existingAbsence = getAbsenceEntityById(absenceId);

        validateOwnership(existingAbsence.getDoctor().getId());

        if (request.getAbsenceDate() != null || request.getStartTime() != null || request.getEndTime() != null) {
            LocalDate newDate = request.getAbsenceDate() != null ?
                    request.getAbsenceDate() : existingAbsence.getAbsenceDate();
            LocalTime newStartTime = request.getStartTime() != null ?
                    request.getStartTime() : existingAbsence.getStartTime();
            LocalTime newEndTime = request.getEndTime() != null ?
                    request.getEndTime() : existingAbsence.getEndTime();

            if (hasConflictingAbsence(existingAbsence.getDoctor().getId(), newDate,
                    newStartTime, newEndTime, absenceId)) {
               throw new CustomException(ErrorCode.ABSENCE_CONFLICT);
            }
        }
        LocalDate originalDate = existingAbsence.getAbsenceDate();
        doctorAbsenceMapper.updateEntityFromRequest(request, existingAbsence);
        DoctorAbsence savedAbsence = doctorAbsenceRepository.save(existingAbsence);

        log.info("Đã cập nhật lịch nghỉ: {}", absenceId);
//        if (request.getAbsenceDate() != null && !request.getAbsenceDate().equals(originalDate)) {
//            handleAffectedAppointmentsAsync(savedAbsence);
//        }
        return doctorAbsenceMapper.toDto(savedAbsence);
    }

    @Override
    public DoctorAbsenceResponse getAbsenceById(UUID absenceId) {
        DoctorAbsence absence = getAbsenceEntityById(absenceId);
        return doctorAbsenceMapper.toDto(absence);
    }

    @Override
    public PageResponse<DoctorAbsenceResponse> getAbsencesByDoctor(UUID doctorUserId, Pageable pageable) {
        Page<DoctorAbsence> absences = doctorAbsenceRepository
                .findByDoctorIdOrderByAbsenceDateDesc(doctorUserId, pageable);
        return pageMapper.toPageResponse(absences, doctorAbsenceMapper::toDto);
    }

    @Override
    public List<DoctorAbsenceResponse> getAbsencesInDateRange(UUID doctorUserId, LocalDate startDate, LocalDate endDate) {
        List<DoctorAbsence> absences = doctorAbsenceRepository
                .findAbsencesInDateRange(doctorUserId, startDate, endDate);
        return doctorAbsenceMapper.toDtoList(absences);
    }

    @Override
    public List<DoctorAbsenceResponse> getFutureAbsences(UUID doctorUserId) {
        List<DoctorAbsence> absences = doctorAbsenceRepository
                .findFutureAbsences(doctorUserId, LocalDate.now());
        return doctorAbsenceMapper.toDtoList(absences);
    }

    @Override
    public void deleteAbsence(UUID absenceId) {
        DoctorAbsence absence = getAbsenceEntityById(absenceId);
        doctorAbsenceRepository.delete(absence);
        log.info("Đã xóa lịch nghỉ: {}", absenceId);
    }

    @Override
    public boolean isDoctorAbsentOnDate(UUID doctorUserId, LocalDate date) {
        List<DoctorAbsence> absences = doctorAbsenceRepository
                .findAbsencesInDateRange(doctorUserId, date, date);
        return !absences.isEmpty();
    }

    @Override
    public int cleanupPastAbsences(LocalDate cutoffDate) {
        int deletedCount = doctorAbsenceRepository.deletePastAbsences(cutoffDate);
        log.info("Đã xóa {} lịch nghỉ trong quá khứ", deletedCount);
        return deletedCount;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleAffectedAppointmentsAsync(DoctorAbsence absence) {
        try {
            handleAffectedAppointments(absence);
        } catch (Exception e) {
            log.error("Lỗi khi xử lý các lịch hẹn bị ảnh hưởng cho lịch nghỉ ID: {}. Chi tiết: {}",
                    absence.getId(), e.getMessage(), e);
        }
    }

    // Xử lý các appointment bị ảnh hưởng khi tạo lịch nghỉ đột xuất
    private void handleAffectedAppointments(DoctorAbsence absence) {
        LocalDate absenceDate = absence.getAbsenceDate();
        UUID doctorUserId = absence.getDoctor().getId();

        if (absenceDate.isBefore(LocalDate.now())) {
            log.info("Ngày nghỉ {} đã qua, bỏ qua việc hủy lịch hẹn", absenceDate);
            return;
        }

        log.info("Đang xử lý các lịch hẹn bị ảnh hưởng của bác sĩ {} vào ngày {}", doctorUserId, absenceDate);

        try {
            // Tìm tất cả appointments của doctor trong ngày nghỉ có trạng thái PENDING hoặc CONFIRMED
            List<Appointment> affectedAppointments = findAffectedAppointments(doctorUserId, absenceDate, absence);

            if (affectedAppointments.isEmpty()) {
                log.info("Không tìm thấy lịch hẹn nào bị ảnh hưởng của bác sĩ {} vào ngày {}", doctorUserId, absenceDate);
                return;
            }

            log.info("Tìm thấy {} lịch hẹn cần hủy và hoàn tiền", affectedAppointments.size());

            // Xử lý từng appointment
            for (Appointment appointment : affectedAppointments) {
                processAffectedAppointmentSafely(appointment, "Bác sĩ nghỉ đột suất");
            }

            log.info("Đã xử lý thành công {} lịch hẹn bị ảnh hưởng cho lịch nghỉ ID: {}",
                    affectedAppointments.size(), absence.getId());

        } catch (Exception e) {
            log.error("Lỗi khi xử lý các lịch hẹn bị ảnh hưởng cho lịch nghỉ ID: {}. Chi tiết: {}",
                    absence.getId(), e.getMessage(), e);
            throw new CustomException(ErrorCode.ABSENCE_APPOINTMENT_PROCESSING_ERROR, e.getMessage());
        }
    }

    // Tìm các appointment bị ảnh hưởng
    private List<Appointment> findAffectedAppointments(UUID doctorUserId, LocalDate absenceDate, DoctorAbsence absence) {
        if (absence.getStartTime() == null && absence.getEndTime() == null) {
            return appointmentRepository.findAppointmentsByDoctorAndDateAndStatus(
                    doctorUserId, absenceDate, Arrays.asList(Status.PENDING, Status.CONFIRMED));
        } else {
            return appointmentRepository.findAppointmentsByDoctorAndDateTimeRangeAndStatus(
                    doctorUserId, absenceDate, absence.getStartTime(), absence.getEndTime(),
                    Arrays.asList(Status.PENDING, Status.CONFIRMED));
        }
    }

    // Xử lý hủy lịch hẹn của từng appointment
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processAffectedAppointmentSafely(Appointment appointment, String reason) {
        try {
            processAffectedAppointment(appointment, reason);
        } catch (Exception e) {
            log.error("Xử lý lịch hẹn ID: {} thất bại. Lỗi: {}",
                    appointment.getId(), e.getMessage(), e);
            // Tiếp tục xử lý các lịch hẹn khác
        }
    }

    /**
     - Xử lý lịch hẹn bị ảnh hưởng bởi lịch nghỉ của bác sĩ
     - Hủy lịch hẹn
     - Giải phóng khung giờ
     - Hoàn tiền cho các thanh toán qua VNPAY
     */
    private void processAffectedAppointment(Appointment appointment, String reason) {
        try {
            log.info("Đang xử lý lịch hẹn ID: {}", appointment.getId());

            // Hủy lịch hẹn
            appointment.setStatus(Status.CANCELLED);
            appointment.setNotes(appointment.getNotes() + " | Đã hủy do: " + reason);
            appointmentRepository.save(appointment);

            // Giải phóng khung giờ với xử lý lỗi tốt hơn
            releaseSlotSafely(appointment);

            // Chỉ hoàn tiền cho các thanh toán qua VNPAY
            refundEligiblePayments(appointment, reason);

            log.info("Đã xử lý thành công lịch hẹn ID: {}", appointment.getId());

        } catch (Exception e) {
            log.error("Lỗi khi xử lý lịch hẹn ID: {}. Chi tiết: {}",
                    appointment.getId(), e.getMessage(), e);
            throw new RuntimeException("Xử lý lịch hẹn thất bại: " + appointment.getId(), e);
        }
    }

    /**
     * Giải phóng khung giờ của lịch hẹn một cách an toàn
     * - Kiểm tra và xử lý an toàn khi khung giờ đã bị giải phóng
     * - Ghi log các bước xử lý
     * - Tiếp tục quy trình hoàn tiền ngay cả khi có lỗi
     */
    private void releaseSlotSafely(Appointment appointment) {
        if (appointment.getSlot() != null) {
            try {
                slotStatusService.reserveSlot(appointment.getSlot().getId());
                log.info("Đã giải phóng khung giờ ID: {} cho lịch hẹn đã hủy: {}",
                        appointment.getSlot().getId(), appointment.getId());
            } catch (Exception e) {
                log.error("Không thể giải phóng khung giờ ID: {} cho lịch hẹn: {}. Lỗi: {}",
                        appointment.getSlot().getId(), appointment.getId(), e.getMessage());

                // Kiểm tra nếu khung giờ đã được giải phóng
                if (e.getMessage() != null && e.getMessage().contains("already available")) {
                    log.info("Khung giờ ID: {} đã được giải phóng trước đó, tiếp tục xử lý...", 
                           appointment.getSlot().getId());
                } else {
                    // Ghi log nhưng không ném ngoại lệ để tránh rollback transaction
                    log.warn("Lỗi không mong muốn khi giải phóng khung giờ, tiếp tục quy trình hoàn tiền");
                }
            }
        }
    }
    /**
     - Hoàn tiền cho các thanh toán qua VNPAY
     - Lấy danh sách thanh toán đã hoàn thành
     - Lọc ra các thanh toán qua VNPAY
     - Xử lý hoàn tiền từng giao dịch
     */
    private void refundEligiblePayments(Appointment appointment, String reason) {
        // Lấy tất cả thanh toán đã hoàn thành
        List<Payment> completedPayments = paymentRepository.findValidPaymentsByAppointmentIdAndStatus(
                appointment.getId(), List.of(PaymentStatus.COMPLETED));

        if (completedPayments.isEmpty()) {
            log.info("Không tìm thấy thanh toán nào cho lịch hẹn ID: {}", appointment.getId());
            return;
        }

        // Lọc chỉ lấy thanh toán qua VNPAY
        List<Payment> vnpayPayments = completedPayments.stream()
                .filter(payment -> isVnpayPayment(payment))
                .collect(Collectors.toList());

        if (vnpayPayments.isEmpty()) {
            log.info("Không tìm thấy thanh toán VNPAY nào cho lịch hẹn ID: {}", appointment.getId());
            return;
        }

        log.info("Tìm thấy {} thanh toán VNPAY cần hoàn tiền cho lịch hẹn ID: {}",
                vnpayPayments.size(), appointment.getId());

        for (Payment payment : vnpayPayments) {
            refundPaymentSafely(payment, appointment, reason);
        }
    }

    /**
     - Xử lý hoàn tiền cho từng giao dịch
     - Tạo yêu cầu hoàn tiền với chính sách hoàn 100%
     - Xử lý trong transaction riêng biệt
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void refundPaymentSafely(Payment payment, Appointment appointment, String reason) {
        try {
            PaymentRefundRequest refundRequest = PaymentRefundRequest.builder()
                    .paymentId(payment.getId())
                    .appointmentId(appointment.getId())
                    .reason(reason + " - Hoàn tiền 100%")
                    .refundType(RefundType.FULL_REFUND)
                    .build();

            paymentService.refundPayment(refundRequest);

            log.info("Đã hoàn tiền thành công cho thanh toán ID: {} của lịch hẹn ID: {}, số tiền: {}",
                    payment.getId(), appointment.getId(), payment.getAmount());

        } catch (Exception e) {
            log.error("Hoàn tiền thất bại cho thanh toán ID: {} của lịch hẹn ID: {}. Lỗi: {}",
                    payment.getId(), appointment.getId(), e.getMessage(), e);
        }
    }

    private boolean isVnpayPayment(Payment payment) {
        // Kiểm tra phương thức thanh toán là VNPAY
        return payment.getPaymentMethod() != null &&
                "VNPAY".contains(payment.getPaymentMethod().name());
    }

    private DoctorAbsence getAbsenceEntityById(UUID absenceId) {
        return doctorAbsenceRepository.findById(absenceId)
                .orElseThrow(() -> new CustomException(ErrorCode.ABSENCE_NOT_FOUND));
    }

    private boolean hasConflictingAbsence(UUID doctorUserId, LocalDate absenceDate,
                                          LocalTime startTime, LocalTime endTime, UUID excludeId) {
        return doctorAbsenceRepository.existsConflictingAbsence(
                doctorUserId, absenceDate, startTime, endTime, excludeId
        );
    }

    private void validateOwnership(UUID doctorUserId) {
        UUID currentUserId = securityUtils.getCurrentUserId();

        if (securityUtils.isCurrentUserAdmin()) {
            return;
        }

        if (!currentUserId.equals(doctorUserId)) {
            log.warn("Người dùng {} đã cố gắng chỉnh sửa lịch nghỉ của bác sĩ {}", currentUserId, doctorUserId);
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }
}
