package org.project.appointment_project.user.service.strategy.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.project.appointment_project.user.model.User;
import org.project.appointment_project.user.repository.UserRepository;
import org.project.appointment_project.user.service.strategy.UserDeletionStrategy;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PatientHardDeletionStrategy implements UserDeletionStrategy {

    UserRepository userRepository;

    @Override
    public boolean deleteUser(User user) {
        //Thêm sau logic check patient có appointment đang diễn ra không
        return true;
    }

    @Override
    public void delete(User user, UUID deleteBy, String reason) {
        userRepository.delete(user);
    }

    @Override
    public String getStrategyName() {
        return "PATIENT_HARD_DELETE";
    }
}
