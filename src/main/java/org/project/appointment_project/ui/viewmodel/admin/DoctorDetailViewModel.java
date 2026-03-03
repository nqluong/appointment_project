package org.project.appointment_project.ui.viewmodel.admin;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.project.appointment_project.user.dto.response.CompleteProfileResponse;
import org.project.appointment_project.user.enums.Gender;
import org.project.appointment_project.user.service.ProfileService;
import org.project.appointment_project.user.utils.NameUtils;
import org.zkoss.bind.annotation.BindingParam;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.ContextParam;
import org.zkoss.bind.annotation.ContextType;
import org.zkoss.bind.annotation.Init;
import org.zkoss.zk.ui.Component;
import org.zkoss.zkplus.spring.SpringUtil;

import lombok.Getter;

public class DoctorDetailViewModel {

    private static final DateTimeFormatter DATE_FMT     = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern(" HH:mm dd/MM/yyyy");

    private ProfileService profileService;

    // Thông tin cá nhân
    @Getter private String fullName = "";
    @Getter private String gender = "";
    @Getter private String dateOfBirth = "";
    @Getter private String phone = "";
    @Getter private String address = "";
    @Getter private String avatarUrl = "";

    // Hồ sơ y tế
    @Getter private String bloodType = "";
    @Getter private String allergies = "";
    @Getter private String medicalHistory = "";
    @Getter private String emergencyContactName = "";
    @Getter private String emergencyContactPhone = "";

    // Thông tin chuyên môn
    @Getter private String licenseNumber = "";
    @Getter private String specialtyName = "";
    @Getter private String qualification = "";
    @Getter private String yearsOfExperience = "";
    @Getter private String consultationFee = "";
    @Getter private String bio = "";
    @Getter private boolean doctorApproved = false;

    // Thời gian cập nhật
    @Getter private String userProfileUpdatedAt = "";
    @Getter private String medicalProfileUpdatedAt = "";

    @Getter private String errorMessage = "";

    @Init
    public void init(@BindingParam("doctorId") UUID doctorId) {
        this.profileService = SpringUtil.getApplicationContext().getBean(ProfileService.class);
        loadDoctor(doctorId);
    }

    private void loadDoctor(UUID doctorId) {
        try {
            CompleteProfileResponse p = profileService.getCompleteProfileInternal(doctorId);

            String first = nvl(p.getFirstName());
            String last  = nvl(p.getLastName());
            fullName    = NameUtils.formatDoctorFullName(first + " " + last);
            gender      = translateGender(p.getGender());
            dateOfBirth = p.getDateOfBirth() != null ? p.getDateOfBirth().format(DATE_FMT) : "";
            phone       = nvl(p.getPhone());
            address     = nvl(p.getAddress());
            avatarUrl   = nvl(p.getAvatarUrl());

            bloodType            = nvl(p.getBloodType());
            allergies            = nvl(p.getAllergies());
            medicalHistory       = nvl(p.getMedicalHistory());
            emergencyContactName = nvl(p.getEmergencyContactName());
            emergencyContactPhone= nvl(p.getEmergencyContactPhone());

            licenseNumber     = nvl(p.getLicenseNumber());
            specialtyName     = nvl(p.getSpecialtyName());
            qualification     = nvl(p.getQualification());
            yearsOfExperience = p.getYearsOfExperience() != null ? p.getYearsOfExperience() + " năm" : "—";
            consultationFee   = p.getConsultationFee() != null ? formatFee(p.getConsultationFee()) : "—";
            bio               = nvl(p.getBio());
            doctorApproved    = p.isDoctorApproved();

            userProfileUpdatedAt    = p.getUserProfileUpdatedAt()    != null ? p.getUserProfileUpdatedAt().format(DATETIME_FMT)    : "";
            medicalProfileUpdatedAt = p.getMedicalProfileUpdatedAt() != null ? p.getMedicalProfileUpdatedAt().format(DATETIME_FMT) : "";

        } catch (Exception e) {
            errorMessage = "Không thể tải thông tin bác sĩ: " + e.getMessage();
        }
    }

    private String translateGender(Gender g) {
        if (g == null) return "";
        return switch (g) {
            case MALE   -> "Nam";
            case FEMALE -> "Nữ";
        };
    }

    private String nvl(String s) { return s != null ? s : ""; }

    private String formatFee(BigDecimal fee) {
        return String.format("%,.0f VNĐ", fee);
    }

    public boolean isHasAvatar()      { return avatarUrl != null && !avatarUrl.isBlank(); }
    public boolean isHasBio()         { return bio       != null && !bio.isBlank(); }
    public boolean isHasMedicalInfo() {
        return (!bloodType.isBlank() || !allergies.isBlank()
                || !medicalHistory.isBlank() || !emergencyContactName.isBlank());
    }

    @Command
    public void closeDialog(@ContextParam(ContextType.VIEW) Component view) {
        view.detach();
    }
}
