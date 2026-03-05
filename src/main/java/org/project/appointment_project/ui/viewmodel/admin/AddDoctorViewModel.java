package org.project.appointment_project.ui.viewmodel.admin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.project.appointment_project.ui.viewmodel.admin.dto.AddDoctorFormData;
import org.project.appointment_project.user.dto.request.DoctorRegistrationRequest;
import org.project.appointment_project.user.dto.response.SpecialtyResponse;
import org.project.appointment_project.user.enums.Gender;
import org.project.appointment_project.user.service.SpecialtyService;
import org.project.appointment_project.user.service.UserRegistrationService;
import org.zkoss.bind.BindUtils;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.ContextParam;
import org.zkoss.bind.annotation.ContextType;
import org.zkoss.bind.annotation.Init;
import org.zkoss.bind.annotation.NotifyChange;
import org.zkoss.zk.ui.Component;
import org.zkoss.zkplus.spring.SpringUtil;
import org.zkoss.zul.ListModelList;

import lombok.Getter;

public class AddDoctorViewModel {

    private UserRegistrationService userRegistrationService;
    private SpecialtyService specialtyService;

    @Getter
    private final AddDoctorFormData form = new AddDoctorFormData();

    @Getter
    private ListModelList<SpecialtyResponse> specialtyModel = new ListModelList<>();
    @Getter
    private final ListModelList<String> genderModel = new ListModelList<>(List.of("Nam", "Nữ"));

    @Getter
    private String errorMessage = "";

    @Getter
    private String successMessage = "";

    @Getter
    private boolean loading = false;


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

        if (!validateForm()) return;

        loading = true;
        try {
            userRegistrationService.registerDoctor(buildRequest());
            successMessage = "Thêm bác sĩ thành công!";
            BindUtils.postGlobalCommand(null, null, "onDoctorAdded", new HashMap<>());
        } catch (Exception e) {
            String msg = e.getMessage();
            errorMessage = (msg != null && !msg.isBlank()) ? msg : "Đã có lỗi xảy ra, vui lòng thử lại";
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
        specialtyModel.clearSelection();
        genderModel.clearSelection();
    }


    private boolean validateForm() {
        if (isBlank(form.getUsername()) || isBlank(form.getEmail()) || isBlank(form.getPassword())
                || isBlank(form.getFirstName()) || isBlank(form.getLastName())
                || isBlank(form.getLicenseNumber()) || isBlank(form.getQualification())) {
            errorMessage = "Vui lòng điền đầy đủ các trường bắt buộc (*)";
            return false;
        }
        if (specialtyModel.getSelection().isEmpty()) {
            errorMessage = "Vui lòng chọn chuyên khoa";
            return false;
        }
        return true;
    }


    private DoctorRegistrationRequest buildRequest() {
        return DoctorRegistrationRequest.builder()
                .username(form.getUsername().trim())
                .email(form.getEmail().trim())
                .password(form.getPassword())
                .firstName(form.getFirstName().trim())
                .lastName(form.getLastName().trim())
                .phone(isBlank(form.getPhone()) ? null : form.getPhone().trim())
                .address(isBlank(form.getAddress()) ? null : form.getAddress().trim())
                .dateOfBirth(parseDateOfBirth())
                .gender(parseGender())
                .licenseNumber(form.getLicenseNumber().trim())
                .specialtyId(specialtyModel.getSelection().iterator().next().getSpecialtyId())
                .qualification(form.getQualification().trim())
                .yearsOfExperience(parseYears())
                .consultationFee(parseFee())
                .bio(isBlank(form.getBio()) ? null : form.getBio().trim())
                .build();
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

    private LocalDate parseDateOfBirth() {
        Date d = form.getDateOfBirth();
        if (d == null) return null;
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private Gender parseGender() {
        if (genderModel.getSelection().isEmpty()) return null;
        return "Nam".equals(genderModel.getSelection().iterator().next()) ? Gender.MALE : Gender.FEMALE;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}

