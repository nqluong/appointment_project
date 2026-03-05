package org.project.appointment_project.user.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.project.appointment_project.user.utils.NameUtils;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DoctorResponse {
    UUID id;
    String fullName;
    String avatarUrl;
    String qualification;
    BigDecimal consultationFee;
    Integer yearsOfExperience;
    String gender;
    String phone;
    String specialtyName;
    boolean isActive;
    boolean isApproved;

    public DoctorResponse(UUID id,
                          String firstName,
                          String lastName,
                          String avatarUrl,
                          String qualification,
                          BigDecimal consultationFee,
                          Integer yearsOfExperience,
                          String gender,
                          String phone,
                          String specialtyName,
                          boolean isActive,
                          boolean isApproved) {
        this.id = id;
        this.fullName = NameUtils.formatDoctorFullName(firstName + " " + lastName);
        this.avatarUrl = avatarUrl;
        this.qualification = qualification;
        this.consultationFee = consultationFee;
        this.yearsOfExperience = yearsOfExperience;
        this.gender = gender;
        this.phone = phone;
        this.specialtyName = specialtyName;
        this.isActive = isActive;
        this.isApproved = isApproved;
    }
}
