package org.project.appointment_project.schedule.repository;

import org.project.appointment_project.schedule.model.DoctorAvailableSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DoctorAvailableSlotRepository extends JpaRepository <DoctorAvailableSlot, UUID> {
}
