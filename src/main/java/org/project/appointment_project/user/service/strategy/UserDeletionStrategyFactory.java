package org.project.appointment_project.user.service.strategy;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.project.appointment_project.user.model.User;
import org.project.appointment_project.user.service.strategy.impl.DoctorSoftDeletionStrategy;
import org.project.appointment_project.user.service.strategy.impl.PatientHardDeletionStrategy;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserDeletionStrategyFactory {

    DoctorSoftDeletionStrategy doctorSoftDeletionStrategy;
    PatientHardDeletionStrategy patientHardDeletionStrategy;

    public UserDeletionStrategy getStrategy(User user, boolean forceHardDelete) {
        if (forceHardDelete) {
            return patientHardDeletionStrategy;
        }

        boolean isDoctor = user.getUserRoles().stream()
                .anyMatch(userRole -> "DOCTOR".equals(userRole.getRole().getName()));

        if (isDoctor) {
            return doctorSoftDeletionStrategy;
        } else {
            return patientHardDeletionStrategy;
        }
    }
}
