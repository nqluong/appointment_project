package org.project.appointment_project.schedule.repository;

import jakarta.persistence.LockModeType;
import org.project.appointment_project.schedule.model.DoctorAvailableSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DoctorAvailableSlotRepository extends JpaRepository <DoctorAvailableSlot, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM DoctorAvailableSlot s WHERE s.id = :slotId")
    Optional<DoctorAvailableSlot> findByIdWithLock(@Param("slotId") UUID slotId);
}
