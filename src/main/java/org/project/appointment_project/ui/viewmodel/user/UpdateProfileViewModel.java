package org.project.appointment_project.ui.viewmodel.user;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.project.appointment_project.ui.viewmodel.user.dto.UpdateProfileFormData;
import org.project.appointment_project.user.dto.request.ProfileUpdateRequest;
import org.project.appointment_project.user.dto.response.CompleteProfileResponse;
import org.project.appointment_project.user.dto.response.SpecialtyResponse;
import org.project.appointment_project.user.enums.Gender;
import org.project.appointment_project.user.service.ProfileService;
import org.project.appointment_project.user.service.SpecialtyService;
import org.zkoss.bind.BindUtils;
import org.zkoss.bind.annotation.*;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Window;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UpdateProfileViewModel {

    private ProfileService profileService;
    private SpecialtyService specialtyService;
    private UUID currentUserId;

    @Getter
    private final UpdateProfileFormData form = new UpdateProfileFormData();

    @Getter
    private final ListModelList<String> genderModel = new ListModelList<>(List.of("Nam", "Nữ"));

    @Getter
    private ListModelList<SpecialtyResponse> specialtyModel = new ListModelList<>();

    @Getter
    private String errorMessage = "";

    @Getter
    private String successMessage = "";

    @Getter
    private boolean loading = false;

    @Getter
    private boolean doctorProfile = false;

    @Getter
    private boolean patientProfile = false;


    @Init
    public void init(@ExecutionArgParam("userId") UUID userId) {
        this.profileService = SpringUtil.getApplicationContext().getBean(ProfileService.class);
        this.specialtyService = SpringUtil.getApplicationContext().getBean(SpecialtyService.class);
        this.currentUserId = userId;
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

            form.setFirstName(nvl(p.getFirstName()));
            form.setLastName(nvl(p.getLastName()));
            form.setPhone(nvl(p.getPhone()));
            form.setAddress(nvl(p.getAddress()));
            form.setAvatarUrl(nvl(p.getAvatarUrl()));
            form.setDateOfBirthStr(p.getDateOfBirth() != null ? p.getDateOfBirth().toString() : "");

            if (p.getGender() != null) {
                String label = p.getGender() == Gender.MALE ? "Nam" : "Nữ";
                genderModel.stream()
                        .filter(label::equals)
                        .findFirst()
                        .ifPresent(genderModel::addToSelection);
            }

            doctorProfile = p.getLicenseNumber() != null && !p.getLicenseNumber().isBlank();
            patientProfile = !doctorProfile;

            if (doctorProfile) {
                form.setLicenseNumber(nvl(p.getLicenseNumber()));
                form.setQualification(nvl(p.getQualification()));
                form.setYearsOfExperienceStr(p.getYearsOfExperience() != null
                        ? String.valueOf(p.getYearsOfExperience()) : "");
                form.setConsultationFeeStr(p.getConsultationFee() != null
                        ? p.getConsultationFee().toPlainString() : "");
                form.setBio(nvl(p.getBio()));

                String currentSpecialty = nvl(p.getSpecialtyName());
                specialtyModel.stream()
                        .filter(s -> s.getName() != null && s.getName().equals(currentSpecialty))
                        .findFirst()
                        .ifPresent(specialtyModel::addToSelection);
            }

            if (patientProfile) {
                form.setBloodType(nvl(p.getBloodType()));
                form.setAllergies(nvl(p.getAllergies()));
                form.setMedicalHistory(nvl(p.getMedicalHistory()));
                form.setEmergencyContactName(nvl(p.getEmergencyContactName()));
                form.setEmergencyContactPhone(nvl(p.getEmergencyContactPhone()));
            }

        } catch (Exception e) {
            log.error("Không thể tải hồ sơ hiện tại: {}", e.getMessage(), e);
            errorMessage = "Không thể tải thông tin hồ sơ: " + e.getMessage();
        }
    }

    @Command
    @NotifyChange({"errorMessage", "loading"})
    public void save(@ContextParam(ContextType.VIEW) Component view) {
        errorMessage = "";
        successMessage = "";
        loading = true;
        try {
            profileService.updateProfileInternal(currentUserId, buildRequest());
            // Mở result dialog TRƯỚC khi detach (còn execution context)
            openResultDialog(true, "Cập nhật hồ sơ thành công!");
            view.detach();
            BindUtils.postGlobalCommand(null, null, "onProfileUpdated", null);
        } catch (IllegalArgumentException e) {
            errorMessage = e.getMessage();
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
    @NotifyChange({"form", "errorMessage", "successMessage"})
    public void resetForm() {
        form.reset();
        errorMessage = "";
        successMessage = "";
        genderModel.clearSelection();
        specialtyModel.clearSelection();
        loadProfile();
    }

    private void openResultDialog(boolean success, String message) {
        Map<String, Object> args = new HashMap<>();
        args.put("success", success);
        args.put("message", message);
        Window win = (Window) Executions.createComponents("/user/result-dialog.zul", null, args);
        win.doModal();
    }

    private ProfileUpdateRequest buildRequest() {
        return ProfileUpdateRequest.builder()
                .firstName(trimOrNull(form.getFirstName()))
                .lastName(trimOrNull(form.getLastName()))
                .phone(trimOrNull(form.getPhone()))
                .address(trimOrNull(form.getAddress()))
                .dateOfBirth(parseDateOfBirth())
                .gender(parseGender())
                .licenseNumber(trimOrNull(form.getLicenseNumber()))
                .specialtyId(parseSpecialtyId())
                .qualification(trimOrNull(form.getQualification()))
                .yearsOfExperience(parseYears())
                .consultationFee(parseFee())
                .bio(trimOrNull(form.getBio()))
                .bloodType(trimOrNull(form.getBloodType()))
                .allergies(trimOrNull(form.getAllergies()))
                .medicalHistory(trimOrNull(form.getMedicalHistory()))
                .emergencyContactName(trimOrNull(form.getEmergencyContactName()))
                .emergencyContactPhone(trimOrNull(form.getEmergencyContactPhone()))
                .build();
    }

    private LocalDate parseDateOfBirth() {
        if (isBlank(form.getDateOfBirthStr())) return null;
        try {
            return LocalDate.parse(form.getDateOfBirthStr().trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Ngày sinh không hợp lệ (định dạng: YYYY-MM-DD)");
        }
    }

    private Gender parseGender() {
        if (genderModel.getSelection().isEmpty()) return null;
        return "Nam".equals(genderModel.getSelection().iterator().next()) ? Gender.MALE : Gender.FEMALE;
    }

    private Integer parseYears() {
        if (isBlank(form.getYearsOfExperienceStr())) return null;
        try {
            return Integer.parseInt(form.getYearsOfExperienceStr().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Số năm kinh nghiệm không hợp lệ");
        }
    }

    private BigDecimal parseFee() {
        if (isBlank(form.getConsultationFeeStr())) return null;
        try {
            return new BigDecimal(form.getConsultationFeeStr().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Giá khám không hợp lệ");
        }
    }

    private String parseSpecialtyId() {
        if (specialtyModel.getSelection().isEmpty()) return null;
        SpecialtyResponse sel = specialtyModel.getSelection().iterator().next();
        return sel.getSpecialtyId() != null ? sel.getSpecialtyId().toString() : null;
    }

    private String trimOrNull(String s) {
        return isBlank(s) ? null : s.trim();
    }

    private String nvl(String s) {
        return s != null ? s : "";
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
