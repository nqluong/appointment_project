package org.project.appointment_project.user.repository;

import org.project.appointment_project.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u " +
            "LEFT JOIN FETCH u.userRoles ur " +
            "LEFT JOIN FETCH ur.role " +
            "WHERE u.username = :username AND u.isActive = true")
    Optional<User> findActiveUserByUsernameWithRoles(@Param("username") String username);


    @Query("SELECT u FROM User u JOIN u.userRoles ur WHERE ur.role.name = :roleName")
    List<User> findByRoleName(@Param("roleName") String roleName);

    @Query("SELECT u FROM User u JOIN u.userRoles ur JOIN u.medicalProfile mp " +
            "WHERE ur.role.name = 'DOCTOR' AND mp.isDoctorApproved = true")
    List<User> findApprovedDoctors();

    @Query("SELECT u FROM User u JOIN u.userRoles ur JOIN u.medicalProfile mp " +
            "WHERE ur.role.name = 'DOCTOR' AND mp.specialty.id = :specialtyId AND mp.isDoctorApproved = true")
    List<User> findApprovedDoctorsBySpecialty(@Param("specialtyId") UUID specialtyId);

}
