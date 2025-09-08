package org.project.appointment_project.schedule.repository;

import org.project.appointment_project.schedule.model.DoctorAbsence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DoctorAbsenceRepository extends JpaRepository<DoctorAbsence, UUID> {
}
