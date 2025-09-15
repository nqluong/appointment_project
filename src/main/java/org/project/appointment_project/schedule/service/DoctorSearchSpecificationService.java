package org.project.appointment_project.schedule.service;

import org.project.appointment_project.schedule.dto.request.DoctorSearchRequest;
import org.project.appointment_project.user.model.User;
import org.springframework.data.jpa.domain.Specification;

public interface DoctorSearchSpecificationService {
    Specification<User> buildDoctorSearchSpecification(DoctorSearchRequest request);

}
