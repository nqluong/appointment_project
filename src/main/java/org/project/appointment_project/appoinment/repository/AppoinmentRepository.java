package org.project.appointment_project.appoinment.repository;

import org.project.appointment_project.appoinment.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AppoinmentRepository extends JpaRepository<Appointment, UUID> {
}
