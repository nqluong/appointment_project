package org.project.appointment_project.ui.viewmodel.admin.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DoctorDetailInfo {

    // Thông tin cá nhân
    @Builder.Default
    String fullName = "";

    @Builder.Default
    String gender = "";

    @Builder.Default
    String dateOfBirth = "";

    @Builder.Default
    String phone = "";

    @Builder.Default
    String address = "";

    @Builder.Default
    String avatarUrl = "";

    // Hồ sơ y tế
    @Builder.Default
    String bloodType = "";

    @Builder.Default
    String allergies = "";

    @Builder.Default
    String medicalHistory = "";

    @Builder.Default
    String emergencyContactName = "";

    @Builder.Default
    String emergencyContactPhone = "";

    // Thông tin chuyên môn
    @Builder.Default
    String licenseNumber = "";

    @Builder.Default
    String specialtyName = "";

    @Builder.Default
    String qualification = "";

    @Builder.Default
    String yearsOfExperience = "";

    @Builder.Default
    String consultationFee = "";

    @Builder.Default
    String bio = "";

    @Builder.Default
    boolean doctorApproved = false;

    @Builder.Default
    String userProfileUpdatedAt = "";

    @Builder.Default
    String medicalProfileUpdatedAt = "";

    public boolean isHasAvatar() {
        return avatarUrl != null && !avatarUrl.isBlank();
    }

    public boolean isHasBio() {
        return bio != null && !bio.isBlank();
    }

    public boolean isHasMedicalInfo() {
        return !bloodType.isBlank() || !allergies.isBlank()
                || !medicalHistory.isBlank() || !emergencyContactName.isBlank();
    }
}

