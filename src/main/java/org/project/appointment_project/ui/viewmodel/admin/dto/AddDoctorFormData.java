package org.project.appointment_project.ui.viewmodel.admin.dto;

import lombok.Data;

import java.util.Date;

@Data
public class AddDoctorFormData {

    private String username = "";
    private String email = "";
    private String password = "";

    private String firstName = "";
    private String lastName = "";
    private String phone = "";
    private String address = "";
    private Date dateOfBirth = null;

    private String licenseNumber = "";
    private String qualification = "";
    private String yearsOfExperienceStr = "";
    private String consultationFeeStr = "";
    private String bio = "";

    public void reset() {
        username = "";
        email = "";
        password = "";
        firstName = "";
        lastName = "";
        phone = "";
        address = "";
        dateOfBirth = null;
        licenseNumber = "";
        qualification = "";
        yearsOfExperienceStr = "";
        consultationFeeStr = "";
        bio = "";
    }
}

