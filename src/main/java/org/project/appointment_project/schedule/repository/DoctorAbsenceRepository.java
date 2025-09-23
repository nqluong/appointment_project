package org.project.appointment_project.schedule.repository;

import org.project.appointment_project.schedule.model.DoctorAbsence;
import org.project.appointment_project.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface DoctorAbsenceRepository extends JpaRepository<DoctorAbsence, UUID> {

    @Query("SELECT da FROM DoctorAbsence da WHERE da.doctor.id = :doctorId " +
            "ORDER BY da.absenceDate DESC, da.startTime ASC")
    Page<DoctorAbsence> findByDoctorIdOrderByAbsenceDateDesc(@Param("doctorId") UUID doctorId, Pageable pageable);


    @Query("SELECT da FROM DoctorAbsence da WHERE da.doctor.id = :doctorUserId " +
            "AND da.absenceDate BETWEEN :startDate AND :endDate " +
            "ORDER BY da.absenceDate ASC, da.startTime ASC")
    List<DoctorAbsence> findAbsencesInDateRange(
            @Param("doctorUserId") UUID doctorUserId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT COUNT(da) > 0 FROM DoctorAbsence da WHERE da.doctor.id = :doctorUserId " +
            "AND da.absenceDate = :absenceDate " +
            "AND (:excludeId IS NULL OR da.id != :excludeId) " +
            "AND (" +
            "   (da.startTime IS NULL AND da.endTime IS NULL) " +
            "   OR (da.startTime IS NOT NULL AND da.endTime IS NOT NULL " +
            "       AND CAST(:startTime AS time) IS NOT NULL AND CAST(:endTime AS time) IS NOT NULL " +
            "       AND CAST(:startTime AS time) < da.endTime AND CAST(:endTime AS time) > da.startTime) " +
            "   OR (da.startTime IS NOT NULL AND da.endTime IS NOT NULL " +
            "       AND CAST(:startTime AS time) IS NULL AND CAST(:endTime AS time) IS NULL) " +
            "   OR (da.startTime IS NULL AND da.endTime IS NULL " +
            "       AND CAST(:startTime AS time) IS NOT NULL AND CAST(:endTime AS time) IS NOT NULL)" +
            ")")
    boolean existsConflictingAbsence(
            @Param("doctorUserId") UUID doctorUserId,
            @Param("absenceDate") LocalDate absenceDate,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludeId") UUID excludeId
    );

    @Query("SELECT da FROM DoctorAbsence da WHERE da.doctor.id = :doctorUserId " +
            "AND da.absenceDate >= :currentDate " +
            "ORDER BY da.absenceDate ASC, da.startTime ASC")
    List<DoctorAbsence> findFutureAbsences(
            @Param("doctorUserId") UUID doctorUserId,
            @Param("currentDate") LocalDate currentDate
    );

    @Modifying
    @Query("DELETE FROM DoctorAbsence da WHERE da.absenceDate < :cutoffDate")
    int deletePastAbsences(@Param("cutoffDate") LocalDate cutoffDate);
}
