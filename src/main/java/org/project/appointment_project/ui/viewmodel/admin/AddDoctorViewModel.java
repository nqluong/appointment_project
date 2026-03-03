package org.project.appointment_project.ui.viewmodel.admin;

import lombok.Getter;
import lombok.Setter;
import org.project.appointment_project.user.dto.request.DoctorRegistrationRequest;
import org.project.appointment_project.user.dto.response.SpecialtyResponse;
import org.project.appointment_project.user.enums.Gender;
import org.project.appointment_project.user.service.SpecialtyService;
import org.project.appointment_project.user.service.UserRegistrationService;
import org.zkoss.bind.BindUtils;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.Init;
import org.zkoss.bind.annotation.NotifyChange;
import org.zkoss.zul.ListModelList;
import org.zkoss.zkplus.spring.SpringUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddDoctorViewModel {

    private UserRegistrationService userRegistrationService;
    private SpecialtyService specialtyService;


    // ===== Form fields =====
    @Getter @Setter private String username = "";
    @Getter @Setter private String email = "";
    @Getter @Setter private String password = "";
    @Getter @Setter private String firstName = "";
    @Getter @Setter private String lastName = "";
    @Getter @Setter private String phone = "";
    @Getter @Setter private String address = "";
    @Getter @Setter private String dateOfBirthStr = "";

    // Specialty listbox
    @Getter private ListModelList<SpecialtyResponse> specialtyModel = new ListModelList<>();

    // Gender listbox
    @Getter private ListModelList<String> genderModel = new ListModelList<>(List.of("Nam", "Nữ"));

    // Doctor specific
    @Getter @Setter private String licenseNumber = "";
    @Getter @Setter private String qualification = "";
    @Getter @Setter private String yearsOfExperienceStr = "";
    @Getter @Setter private String consultationFeeStr = "";
    @Getter @Setter private String bio = "";

    // UI state
    @Getter private String errorMessage = "";
    @Getter private String successMessage = "";
    @Getter private boolean loading = false;

    @Init
    public void init() {
        this.userRegistrationService = SpringUtil.getApplicationContext()
                .getBean(UserRegistrationService.class);
        this.specialtyService = SpringUtil.getApplicationContext()
                .getBean(SpecialtyService.class);
        loadSpecialties();
    }

    private void loadSpecialties() {
        try {
            List<SpecialtyResponse> list = specialtyService.getAllActiveSpecialties();
            specialtyModel = new ListModelList<>(list != null ? list : new ArrayList<>());
            specialtyModel.setMultiple(false);
        } catch (Exception e) {
            specialtyModel = new ListModelList<>();
        }
    }

    @Command
    @NotifyChange({"errorMessage", "successMessage", "loading"})
    public void save() {
        errorMessage = "";
        successMessage = "";

        // Validate bắt buộc
        if (isBlank(username) || isBlank(email) || isBlank(password)
                || isBlank(firstName) || isBlank(lastName)
                || isBlank(licenseNumber) || isBlank(qualification)) {
            errorMessage = "Vui lòng điền đầy đủ các trường bắt buộc (*)";
            return;
        }

        if (specialtyModel.getSelection().isEmpty()) {
            errorMessage = "Vui lòng chọn chuyên khoa";
            return;
        }

        loading = true;
        try {
            SpecialtyResponse selectedSpecialty = specialtyModel.getSelection().iterator().next();

            // Parse optional fields
            Integer years = null;
            if (!isBlank(yearsOfExperienceStr)) {
                try { years = Integer.parseInt(yearsOfExperienceStr.trim()); }
                catch (NumberFormatException ex) { errorMessage = "Số năm kinh nghiệm không hợp lệ"; loading = false; return; }
            }

            BigDecimal fee = null;
            if (!isBlank(consultationFeeStr)) {
                try { fee = new BigDecimal(consultationFeeStr.trim()); }
                catch (NumberFormatException ex) { errorMessage = "Giá khám không hợp lệ"; loading = false; return; }
            }

            LocalDate dob = null;
            if (!isBlank(dateOfBirthStr)) {
                try { dob = LocalDate.parse(dateOfBirthStr.trim()); }
                catch (Exception ex) { errorMessage = "Ngày sinh không hợp lệ (định dạng: YYYY-MM-DD)"; loading = false; return; }
            }

            Gender gender = null;
            if (!genderModel.getSelection().isEmpty()) {
                gender = "Nam".equals(genderModel.getSelection().iterator().next()) ? Gender.MALE : Gender.FEMALE;
            }

            DoctorRegistrationRequest request = DoctorRegistrationRequest.builder()
                    .username(username.trim())
                    .email(email.trim())
                    .password(password)
                    .firstName(firstName.trim())
                    .lastName(lastName.trim())
                    .phone(isBlank(phone) ? null : phone.trim())
                    .address(isBlank(address) ? null : address.trim())
                    .dateOfBirth(dob)
                    .gender(gender)
                    .licenseNumber(licenseNumber.trim())
                    .specialtyId(selectedSpecialty.getSpecialtyId())
                    .qualification(qualification.trim())
                    .yearsOfExperience(years)
                    .consultationFee(fee)
                    .bio(isBlank(bio) ? null : bio.trim())
                    .build();

            userRegistrationService.registerDoctor(request);
            successMessage = "Thêm bác sĩ thành công!";

            // Notify DoctorListViewModel reload
            Map<String, Object> args = new HashMap<>();
            args.put("refreshDoctors", true);
            BindUtils.postGlobalCommand(null, null, "onDoctorAdded", args);

        } catch (Exception e) {
            String msg = e.getMessage();
            errorMessage = (msg != null && !msg.isBlank()) ? msg : "Đã có lỗi xảy ra, vui lòng thử lại";
        } finally {
            loading = false;
        }
    }

    @Command
    public void closeDialog() {
        // Tìm window cha và đóng
        Map<String, Object> args = new HashMap<>();
        BindUtils.postGlobalCommand(null, null, "closeAddDoctorDialog", args);
    }

    @Command
    @NotifyChange({"username","email","password","firstName","lastName","phone",
            "address","dateOfBirthStr","licenseNumber","qualification",
            "yearsOfExperienceStr","consultationFeeStr","bio","errorMessage","successMessage"})
    public void resetForm() {
        username = ""; email = ""; password = ""; firstName = ""; lastName = "";
        phone = ""; address = ""; dateOfBirthStr = ""; licenseNumber = "";
        qualification = ""; yearsOfExperienceStr = ""; consultationFeeStr = ""; bio = "";
        errorMessage = ""; successMessage = "";
        specialtyModel.clearSelection();
        genderModel.clearSelection();
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}

