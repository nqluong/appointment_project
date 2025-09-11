package org.project.appointment_project.user.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.project.appointment_project.user.enums.Gender;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateCompleteProfileRequest {

    //User profile
    @NotBlank(message = "First name cannot be empty")
    @Size(max = 100, message = "First name cannot exceed 100 characters")
    @Pattern(regexp = "^[\\p{L}\\s]+$", message = "First name can only contain letters and spaces")
    String firstName;

    @NotBlank(message = "Last name cannot be empty")
    @Size(max = 100, message = "Last name cannot exceed 100 characters")
    @Pattern(regexp = "^[\\p{L}\\s]+$", message = "Last name can only contain letters and spaces")
    String lastName;

    LocalDate dateOfBirth;
    Gender gender;
    String address;

    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Invalid phone number")
    @Size(max = 20, message = "Phone number cannot exceed 20 characters")
    String phone;

    String avatarUrl;

    // MedicalProfile fields - will be populated based on user role
    @Pattern(regexp = "^(A|B|AB|O)[+-]$", message = "Invalid blood type")
    String bloodType;

    String allergies;
    String medicalHistory;

    @Size(max = 100, message = "Emergency contact name cannot exceed 100 characters")
    @Pattern(regexp = "^[\\p{L}\\s]+$", message = "Name can only contain letters and spaces")
    String emergencyContactName;

    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Invalid emergency contact phone number")
    @Size(max = 20, message = "Emergency contact phone number cannot exceed 20 characters")
    String emergencyContactPhone;

    // Doctor-specific fields
    @Size(max = 50, message = "License number cannot exceed 50 characters")
    @Pattern(regexp = "^[A-Z0-9]+$", message = "License number can only contain uppercase letters and numbers")
    String licenseNumber;

    String qualification;

    @Min(value = 0, message = "Years of experience cannot be negative")
    Integer yearsOfExperience;

    @DecimalMin(value = "0.0", inclusive = false, message = "Consultation fee must be positive")
    BigDecimal consultationFee;

    String bio;
    String specialtyId;
}
