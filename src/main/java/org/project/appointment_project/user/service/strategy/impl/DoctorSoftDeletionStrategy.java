package org.project.appointment_project.user.service.strategy.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.user.model.User;
import org.project.appointment_project.user.repository.UserRepository;
import org.project.appointment_project.user.service.strategy.UserDeletionStrategy;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DoctorSoftDeletionStrategy implements UserDeletionStrategy {

    UserRepository userRepository;

    @Override
    public boolean deleteUser(User user) {

        //Thêm logic xử lý nếu có appointment sắp tới
        return user.getDeletedAt() == null ;
    }

    @Override
    public void delete(User user, UUID deleteBy, String reason) {
        user.setDeletedAt(LocalDateTime.now());
        user.setDeletedBy(deleteBy);
        user.setActive(false);

        if(user.getMedicalProfile() != null) {
            user.getMedicalProfile().setDoctorApproved(false);
        }

        user.getUserRoles().forEach(userRole -> {
            if("DOCTOR".equals(userRole.getRole().getName())) {
                userRole.setActive(false);
            }
        });

        userRepository.save(user);
    }

    @Override
    public String getStrategyName() {
        return "DOCTOR_SOFT_DELETE";
    }
}
