package org.project.appointment_project.ui.viewmodel.user.dto;

import lombok.Data;

@Data
public class UpdateProfileFormData {

    private String firstName = "";
    private String lastName = "";
    private String phone = "";
    private String address = "";
    private String dateOfBirthStr = "";
    private String avatarUrl = "";

    private String bloodType = "";
    private String allergies = "";
    private String medicalHistory = "";
    private String emergencyContactName = "";
    private String emergencyContactPhone = "";

    private String licenseNumber = "";
    private String qualification = "";
    private String yearsOfExperienceStr = "";
    private String consultationFeeStr = "";
    private String bio = "";

    public void reset() {
        firstName = "";
        lastName = "";
        phone = "";
        address = "";
        dateOfBirthStr = "";
        avatarUrl = "";
        bloodType = "";
        allergies = "";
        medicalHistory = "";
        emergencyContactName = "";
        emergencyContactPhone = "";
        licenseNumber = "";
        qualification = "";
        yearsOfExperienceStr = "";
        consultationFeeStr = "";
        bio = "";
    }
}

