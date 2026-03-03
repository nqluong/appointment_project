package org.project.appointment_project.ui.viewmodel.user;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.project.appointment_project.user.dto.request.ProfileUpdateRequest;
import org.project.appointment_project.user.dto.response.CompleteProfileResponse;
import org.project.appointment_project.user.dto.response.SpecialtyResponse;
import org.project.appointment_project.user.enums.Gender;
import org.project.appointment_project.user.service.ProfileService;
import org.project.appointment_project.user.service.SpecialtyService;
import org.zkoss.bind.annotation.*;
import org.zkoss.zk.ui.Component;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.ListModelList;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UpdateProfileViewModel {

    private ProfileService profileService;
    private SpecialtyService specialtyService;
    private UUID currentUserId;

    // ===== Thông tin cá nhân =====
    @Getter @Setter private String firstName = "";
    @Getter @Setter private String lastName = "";
    @Getter @Setter private String phone = "";
    @Getter @Setter private String address = "";
    @Getter @Setter private String dateOfBirthStr = "";
    @Getter @Setter private String avatarUrl = "";

    // ===== Giới tính =====
    @Getter private ListModelList<String> genderModel = new ListModelList<>(List.of("Nam", "Nữ"));

    // ===== Hồ sơ y tế (bệnh nhân) =====
    @Getter @Setter private String bloodType = "";
    @Getter @Setter private String allergies = "";
    @Getter @Setter private String medicalHistory = "";
    @Getter @Setter private String emergencyContactName = "";
    @Getter @Setter private String emergencyContactPhone = "";

    // ===== Thông tin chuyên môn (bác sĩ) =====
    @Getter @Setter private String licenseNumber = "";
    @Getter @Setter private String qualification = "";
    @Getter @Setter private String yearsOfExperienceStr = "";
    @Getter @Setter private String consultationFeeStr = "";
    @Getter @Setter private String bio = "";

    @Getter private ListModelList<SpecialtyResponse> specialtyModel = new ListModelList<>();

    @Getter private String errorMessage = "";
    @Getter private String successMessage = "";
    @Getter private boolean loading = false;
    @Getter private boolean doctorProfile = false;
    @Getter private boolean patientProfile = false;

    @Init
    public void init(@ExecutionArgParam("userId") UUID userId) {
        this.profileService   = SpringUtil.getApplicationContext().getBean(ProfileService.class);
        this.specialtyService = SpringUtil.getApplicationContext().getBean(SpecialtyService.class);
        this.currentUserId    = userId;

        loadSpecialties();
        loadProfile();
    }

    private void loadSpecialties() {
        try {
            List<SpecialtyResponse> list = specialtyService.getAllActiveSpecialties();
            specialtyModel = new ListModelList<>(list != null ? list : new ArrayList<>());
            specialtyModel.setMultiple(false);
        } catch (Exception e) {
            log.warn("Không tải được danh sách chuyên khoa: {}", e.getMessage());
            specialtyModel = new ListModelList<>();
        }
    }

    private void loadProfile() {
        try {
            CompleteProfileResponse p = profileService.getCompleteProfileInternal(currentUserId);

            firstName      = nvl(p.getFirstName());
            lastName       = nvl(p.getLastName());
            phone          = nvl(p.getPhone());
            address        = nvl(p.getAddress());
            avatarUrl      = nvl(p.getAvatarUrl());
            dateOfBirthStr = p.getDateOfBirth() != null ? p.getDateOfBirth().toString() : "";

            if (p.getGender() != null) {
                String gLabel = p.getGender() == Gender.MALE ? "Nam" : "Nữ";
                genderModel.stream()
                        .filter(g -> g.equals(gLabel))
                        .findFirst()
                        .ifPresent(g -> genderModel.addToSelection(g));
            }

            // Xác định loại profile
            doctorProfile  = p.getLicenseNumber() != null && !p.getLicenseNumber().isBlank();
            patientProfile = !doctorProfile;

            if (doctorProfile) {
                licenseNumber       = nvl(p.getLicenseNumber());
                qualification       = nvl(p.getQualification());
                yearsOfExperienceStr = p.getYearsOfExperience() != null ? String.valueOf(p.getYearsOfExperience()) : "";
                consultationFeeStr  = p.getConsultationFee() != null ? p.getConsultationFee().toPlainString() : "";
                bio                 = nvl(p.getBio());

                // Chọn chuyên khoa hiện tại
                String currentSpecialty = nvl(p.getSpecialtyName());
                specialtyModel.stream()
                        .filter(s -> s.getName() != null && s.getName().equals(currentSpecialty))
                        .findFirst()
                        .ifPresent(s -> specialtyModel.addToSelection(s));
            }

            if (patientProfile) {
                bloodType             = nvl(p.getBloodType());
                allergies             = nvl(p.getAllergies());
                medicalHistory        = nvl(p.getMedicalHistory());
                emergencyContactName  = nvl(p.getEmergencyContactName());
                emergencyContactPhone = nvl(p.getEmergencyContactPhone());
            }

        } catch (Exception e) {
            log.error("Không thể tải hồ sơ hiện tại: {}", e.getMessage(), e);
            errorMessage = "Không thể tải thông tin hồ sơ: " + e.getMessage();
        }
    }

    @Command
    @NotifyChange({"errorMessage", "successMessage", "loading"})
    public void save() {
        errorMessage = "";
        successMessage = "";
        loading = true;

        try {
            // Parse ngày sinh
            LocalDate dob = null;
            if (!isBlank(dateOfBirthStr)) {
                try {
                    dob = LocalDate.parse(dateOfBirthStr.trim());
                } catch (DateTimeParseException ex) {
                    errorMessage = "Ngày sinh không hợp lệ (định dạng: YYYY-MM-DD)";
                    return;
                }
            }

            // Parse giới tính
            Gender gender = null;
            if (!genderModel.getSelection().isEmpty()) {
                String sel = genderModel.getSelection().iterator().next();
                gender = "Nam".equals(sel) ? Gender.MALE : Gender.FEMALE;
            }

            // Parse yearsOfExperience
            Integer years = null;
            if (!isBlank(yearsOfExperienceStr)) {
                try {
                    years = Integer.parseInt(yearsOfExperienceStr.trim());
                } catch (NumberFormatException ex) {
                    errorMessage = "Số năm kinh nghiệm không hợp lệ";
                    return;
                }
            }

            // Parse consultationFee
            BigDecimal fee = null;
            if (!isBlank(consultationFeeStr)) {
                try {
                    fee = new BigDecimal(consultationFeeStr.trim());
                } catch (NumberFormatException ex) {
                    errorMessage = "Giá khám không hợp lệ";
                    return;
                }
            }

            // Lấy specialtyId đã chọn
            String specialtyId = null;
            if (!specialtyModel.getSelection().isEmpty()) {
                SpecialtyResponse sel = specialtyModel.getSelection().iterator().next();
                specialtyId = sel.getSpecialtyId() != null ? sel.getSpecialtyId().toString() : null;
            }

            ProfileUpdateRequest request = ProfileUpdateRequest.builder()
                    .firstName(isBlank(firstName) ? null : firstName.trim())
                    .lastName(isBlank(lastName) ? null : lastName.trim())
                    .dateOfBirth(dob)
                    .gender(gender)
                    .phone(isBlank(phone) ? null : phone.trim())
                    .address(isBlank(address) ? null : address.trim())
                    // Doctor fields
                    .licenseNumber(isBlank(licenseNumber) ? null : licenseNumber.trim())
                    .specialtyId(specialtyId)
                    .qualification(isBlank(qualification) ? null : qualification.trim())
                    .yearsOfExperience(years)
                    .consultationFee(fee)
                    .bio(isBlank(bio) ? null : bio.trim())
                    // Patient fields
                    .bloodType(isBlank(bloodType) ? null : bloodType.trim())
                    .allergies(isBlank(allergies) ? null : allergies.trim())
                    .medicalHistory(isBlank(medicalHistory) ? null : medicalHistory.trim())
                    .emergencyContactName(isBlank(emergencyContactName) ? null : emergencyContactName.trim())
                    .emergencyContactPhone(isBlank(emergencyContactPhone) ? null : emergencyContactPhone.trim())
                    .build();

            profileService.updateProfileInternal(currentUserId, request);
            successMessage = "Cập nhật hồ sơ thành công!";

        } catch (Exception e) {
            String msg = e.getMessage();
            errorMessage = (msg != null && !msg.isBlank()) ? msg : "Đã có lỗi xảy ra, vui lòng thử lại.";
            log.error("Profile update failed for userId={}: {}", currentUserId, msg, e);
        } finally {
            loading = false;
        }
    }

    @Command
    public void closeDialog(@ContextParam(ContextType.VIEW) Component view) {
        view.detach();
    }

    @Command
    @NotifyChange({"firstName","lastName","phone","address","dateOfBirthStr",
                   "bloodType","allergies","medicalHistory",
                   "emergencyContactName","emergencyContactPhone",
                   "licenseNumber","qualification","yearsOfExperienceStr",
                   "consultationFeeStr","bio","errorMessage","successMessage"})
    public void resetForm() {
        errorMessage = "";
        successMessage = "";
        genderModel.clearSelection();
        specialtyModel.clearSelection();
        loadProfile();
    }

    private String nvl(String s) { return s != null ? s : ""; }
    private boolean isBlank(String s) { return s == null || s.isBlank(); }
}
