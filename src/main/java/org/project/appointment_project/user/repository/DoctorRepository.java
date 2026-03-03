package org.project.appointment_project.user.repository;

import org.project.appointment_project.user.dto.response.DoctorResponse;
import org.project.appointment_project.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DoctorRepository extends JpaRepository<User, UUID> {

    @Query("SELECT new org.project.appointment_project.user.dto.response.DoctorResponse(" +
            "u.id, up.firstName, up.lastName, up.avatarUrl, " +
            "mp.qualification, mp.consultationFee, mp.yearsOfExperience, " +
            "CAST(up.gender AS string), up.phone, s.name) " +
            "FROM User u " +
            "JOIN u.userRoles ur " +
            "JOIN ur.role r " +
            "JOIN u.userProfile up " +
            "JOIN u.medicalProfile mp " +
            "LEFT JOIN mp.specialty s " +
            "WHERE r.name = 'DOCTOR' " +
            "AND u.isActive = true " +
            "AND ur.isActive = true " +
            "AND mp.isDoctorApproved = true")
    Page<DoctorResponse> findAllApprovedDoctors(Pageable pageable);

    @Query("SELECT new org.project.appointment_project.user.dto.response.DoctorResponse(" +
            "u.id, up.firstName, up.lastName, up.avatarUrl, " +
            "mp.qualification, mp.consultationFee, mp.yearsOfExperience, " +
            "CAST(up.gender AS string), up.phone, s.name) " +
            "FROM User u " +
            "JOIN u.userRoles ur " +
            "JOIN ur.role r " +
            "JOIN u.userProfile up " +
            "JOIN u.medicalProfile mp " +
            "LEFT JOIN mp.specialty s " +
            "WHERE r.name = 'DOCTOR' " +
            "AND u.isActive = true " +
            "AND ur.isActive = true " +
            "AND mp.isDoctorApproved = true " +
            "AND (COALESCE(:specialtyName, '') = '' OR LOWER(s.name) LIKE LOWER(CONCAT('%', :specialtyName, '%')))")
    Page<DoctorResponse> findDoctorsWithFilters(@Param("specialtyName") String specialtyName, Pageable pageable);

    @Query("SELECT DISTINCT u FROM User u " +
            "JOIN u.userRoles ur " +
            "JOIN ur.role r " +
            "JOIN u.userProfile up " +
            "JOIN u.medicalProfile mp " +
            "LEFT JOIN mp.specialty s " +
            "WHERE r.name = 'DOCTOR' " +
            "AND u.isActive = true " +
            "AND ur.isActive = true " +
            "AND mp.isDoctorApproved = true")
    Page<User> findAllApprovedDoctors2(Pageable pageable);
}
