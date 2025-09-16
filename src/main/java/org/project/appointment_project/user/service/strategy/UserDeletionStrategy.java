package org.project.appointment_project.user.service.strategy;

import org.project.appointment_project.user.model.User;

import java.util.UUID;

public interface UserDeletionStrategy {

    boolean deleteUser(User user);

    void delete(User user, UUID deleteBy, String reason);

    String getStrategyName();
}
