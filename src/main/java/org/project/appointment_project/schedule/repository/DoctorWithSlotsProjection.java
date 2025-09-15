package org.project.appointment_project.schedule.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public interface DoctorWithSlotsProjection {
    UUID getUserId();
    String getFirstName();
    String getLastName();
    String getPhone();
    String getEmail();
    LocalDate getDateOfBirth();
    String getAvatarUrl();
    String getGender();


    String getSpecialtyName();
    String getLicenseNumber();
    String getQualification();
    Integer getYearsOfExperience();
    BigDecimal getConsultationFee();

    List<SlotProjection> getSlots();
}
